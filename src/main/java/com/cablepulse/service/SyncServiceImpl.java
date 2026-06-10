package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.*;
import com.cablepulse.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SyncServiceImpl implements SyncService {

    private final OfflineSyncQueueRepository offlineSyncQueueRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PaymentProcessingService paymentProcessingService;
    private final ObjectMapper objectMapper;

    public SyncServiceImpl(
            OfflineSyncQueueRepository offlineSyncQueueRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            PaymentProcessingService paymentProcessingService,
            ObjectMapper objectMapper) {
        this.offlineSyncQueueRepository = offlineSyncQueueRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.paymentProcessingService = paymentProcessingService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public StandardResponse_SyncResolution processSyncBatch(SyncRequestPayload payload) {
        List<String> processedEventIds = new ArrayList<>();
        List<String> rejectedEventIds = new ArrayList<>();

        if (payload == null || payload.events() == null) {
            SyncResolutionDTO resolution = new SyncResolutionDTO(processedEventIds, rejectedEventIds);
            return new StandardResponse_SyncResolution(
                    LocalDateTime.now(),
                    "SUCCESS",
                    null,
                    new SyncResolutionInner(resolution)
            );
        }

        for (SyncEventDTO event : payload.events()) {
            UUID token = event.idempotencyToken();
            if (token == null) {
                rejectedEventIds.add(event.eventId());
                continue;
            }
            String tokenValue = token.toString();

            String payloadBody;
            try {
                payloadBody = objectMapper.writeValueAsString(event.payload());
            } catch (Exception e) {
                payloadBody = "{}";
            }

            Optional<OfflineSyncQueue> existingQueue = offlineSyncQueueRepository.findByIdempotencyToken(tokenValue);

            if (existingQueue.isPresent() && "SUCCESS".equals(existingQueue.get().getStatus())) {
                processedEventIds.add(event.eventId());
                continue; // Skip reprocessing
            }

            try {
                // Read and validate the payload body
                Map<String, Object> payloadMap = event.payload();
                if (payloadMap == null) {
                    throw new IllegalArgumentException("Payload body cannot be null");
                }

                String customerId = (String) payloadMap.get("customerId");
                if (customerId == null) {
                    throw new IllegalArgumentException("customerId is required in payload");
                }

                customerRepository.findById(customerId)
                        .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));

                List<String> months = extractMonths(payloadMap);
                if (months.isEmpty()) {
                    throw new IllegalArgumentException("At least one payment billing month is required");
                }

                BigDecimal amountCollected = BigDecimal.ZERO;
                Object amountObj = payloadMap.get("amount");
                if (amountObj == null) {
                    amountObj = payloadMap.get("grossAmountCollected");
                }
                if (amountObj != null) {
                    amountCollected = new BigDecimal(String.valueOf(amountObj));
                }

                PaymentMode paymentMode = PaymentMode.CASH;
                String paymentModeStr = (String) payloadMap.get("modeOfPayment");
                if (paymentModeStr == null) {
                    paymentModeStr = (String) payloadMap.get("paymentMode");
                }
                if (paymentModeStr != null) {
                    try {
                        paymentMode = PaymentMode.valueOf(paymentModeStr.toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                        if ("UPI".equalsIgnoreCase(paymentModeStr)) {
                            paymentMode = PaymentMode.ONLINE_UPI;
                        }
                    }
                }

                Employee fieldAgent = resolveFieldAgent((String) payloadMap.get("fieldAgentId"));

                int year = java.time.LocalDate.now().getYear();
                Object yearObj = payloadMap.get("year");
                if (yearObj != null) {
                    year = Integer.parseInt(String.valueOf(yearObj));
                }

                paymentProcessingService.recordPayment(
                        customerId,
                        amountCollected,
                        months,
                        year,
                        paymentMode,
                        fieldAgent
                );

                // Save SUCCESS status tracker in Offline Sync Queue
                OfflineSyncQueue queueEntry = existingQueue.orElse(new OfflineSyncQueue(event.eventId(), tokenValue, payloadBody, "SUCCESS", LocalDateTime.now()));
                queueEntry.setStatus("SUCCESS");
                queueEntry.setProcessedAt(LocalDateTime.now());
                offlineSyncQueueRepository.save(queueEntry);

                processedEventIds.add(event.eventId());

            } catch (Exception e) {
                // If validation or database execution fails, capture and log FAILED state
                OfflineSyncQueue queueEntry = existingQueue.orElse(new OfflineSyncQueue(event.eventId(), tokenValue, payloadBody, "FAILED", LocalDateTime.now()));
                queueEntry.setStatus("FAILED");
                queueEntry.setProcessedAt(LocalDateTime.now());
                offlineSyncQueueRepository.save(queueEntry);

                rejectedEventIds.add(event.eventId());
            }
        }

        SyncResolutionDTO resolution = new SyncResolutionDTO(processedEventIds, rejectedEventIds);
        return new StandardResponse_SyncResolution(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                new SyncResolutionInner(resolution)
        );
    }

    private List<String> extractMonths(Map<String, Object> payloadMap) {
        Object monthsObj = payloadMap.get("months");
        if (monthsObj == null) {
            monthsObj = payloadMap.get("monthsPaid");
        }
        List<String> months = new ArrayList<>();
        if (monthsObj instanceof List) {
            for (Object m : (List<?>) monthsObj) {
                months.add(String.valueOf(m).toUpperCase());
            }
        } else if (monthsObj instanceof String) {
            months.add(String.valueOf(monthsObj).toUpperCase());
        }
        return months;
    }

    private Employee resolveFieldAgent(String agentId) {
        if (agentId != null) {
            Employee found = employeeRepository.findById(agentId).orElse(null);
            if (found != null) {
                return found;
            }
        }
        List<Employee> employees = employeeRepository.findAll();
        if (!employees.isEmpty()) {
            return employees.get(0);
        }
        Employee systemAgent = new Employee("sys-agent", "System Sync Agent", EmployeeRole.COLLECTION_BOY);
        return employeeRepository.save(systemAgent);
    }
}
