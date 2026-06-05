package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.*;
import com.cablepulse.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SyncServiceImpl implements SyncService {

    private final OfflineSyncQueueRepository offlineSyncQueueRepository;
    private final CustomerRepository customerRepository;
    private final CustomerLedgerRepository customerLedgerRepository;
    private final DailyTransactionRepository dailyTransactionRepository;
    private final EmployeeRepository employeeRepository;

    public SyncServiceImpl(
            OfflineSyncQueueRepository offlineSyncQueueRepository,
            CustomerRepository customerRepository,
            CustomerLedgerRepository customerLedgerRepository,
            DailyTransactionRepository dailyTransactionRepository,
            EmployeeRepository employeeRepository) {
        this.offlineSyncQueueRepository = offlineSyncQueueRepository;
        this.customerRepository = customerRepository;
        this.customerLedgerRepository = customerLedgerRepository;
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.employeeRepository = employeeRepository;
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

            Optional<OfflineSyncQueue> existingQueue = offlineSyncQueueRepository.findByIdempotencyToken(token);

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

                Customer customer = customerRepository.findById(customerId)
                        .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));

                // Extract months
                Object monthsObj = payloadMap.get("months");
                List<String> months = new ArrayList<>();
                if (monthsObj instanceof List) {
                    for (Object m : (List<?>) monthsObj) {
                        months.add(String.valueOf(m).toUpperCase());
                    }
                } else if (monthsObj instanceof String) {
                    months.add(String.valueOf(monthsObj).toUpperCase());
                }

                if (months.isEmpty()) {
                    throw new IllegalArgumentException("At least one payment billing month is required");
                }

                // Extract amount
                BigDecimal amountCollected = BigDecimal.ZERO;
                Object amountObj = payloadMap.get("amount");
                if (amountObj != null) {
                    amountCollected = new BigDecimal(String.valueOf(amountObj));
                }

                // Extract payment mode
                String paymentModeStr = (String) payloadMap.get("modeOfPayment");
                PaymentMode paymentMode = PaymentMode.CASH;
                if (paymentModeStr != null) {
                    try {
                        paymentMode = PaymentMode.valueOf(paymentModeStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // Keep default mode
                    }
                }

                // Extract or find field agent
                String agentId = (String) payloadMap.get("fieldAgentId");
                Employee fieldAgent = null;
                if (agentId != null) {
                    fieldAgent = employeeRepository.findById(agentId).orElse(null);
                }
                if (fieldAgent == null) {
                    // Load the first available employee or create a default system agent
                    List<Employee> employees = employeeRepository.findAll();
                    if (!employees.isEmpty()) {
                        fieldAgent = employees.get(0);
                    } else {
                        fieldAgent = new Employee("sys-agent", "System Sync Agent", EmployeeRole.COLLECTION_BOY);
                        employeeRepository.save(fieldAgent);
                    }
                }

                // Update ledger records
                BigDecimal amountPerMonth = amountCollected.divide(BigDecimal.valueOf(months.size()), 2, RoundingMode.HALF_UP);
                List<CustomerLedger> ledgers = customerLedgerRepository.findByCustomer_CustomerId(customerId);

                for (String month : months) {
                    Optional<CustomerLedger> existingLedger = ledgers.stream()
                            .filter(l -> l.getBillingMonth().equalsIgnoreCase(month) && l.getBillingYear() == 2026)
                            .findFirst();

                    if (existingLedger.isPresent()) {
                        CustomerLedger ledger = existingLedger.get();
                        ledger.setStatus(LedgerStatus.PAID);
                        ledger.setPaidAmount(ledger.getPaidAmount().add(amountPerMonth));
                        ledger.setDueAmount(BigDecimal.ZERO);
                        customerLedgerRepository.save(ledger);
                    } else {
                        CustomerLedger ledger = new CustomerLedger(
                                month,
                                2026,
                                LedgerStatus.PAID,
                                amountPerMonth,
                                BigDecimal.ZERO,
                                customer
                        );
                        customerLedgerRepository.save(ledger);
                    }
                }

                // Log the daily transaction
                DailyTransaction transaction = new DailyTransaction(
                        UUID.randomUUID().toString(),
                        amountCollected,
                        paymentMode,
                        LocalDateTime.now(),
                        customer,
                        fieldAgent
                );
                dailyTransactionRepository.save(transaction);

                // Save SUCCESS status tracker in Offline Sync Queue
                OfflineSyncQueue queueEntry = existingQueue.orElse(new OfflineSyncQueue(token, "SUCCESS", event.eventId(), LocalDateTime.now()));
                queueEntry.setStatus("SUCCESS");
                queueEntry.setProcessedAt(LocalDateTime.now());
                offlineSyncQueueRepository.save(queueEntry);

                processedEventIds.add(event.eventId());

            } catch (Exception e) {
                // If validation or database execution fails, capture and log FAILED state
                OfflineSyncQueue queueEntry = existingQueue.orElse(new OfflineSyncQueue(token, "FAILED", event.eventId(), LocalDateTime.now()));
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
}
