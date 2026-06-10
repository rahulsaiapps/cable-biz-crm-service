package com.cablepulse.controller;

import com.cablepulse.dto.CreateCustomerRequestDto;
import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.Customer;
import com.cablepulse.model.CustomerLedger;
import com.cablepulse.repository.CustomerLedgerRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.service.CustomerRegistrationService;
import com.cablepulse.service.CustomerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CustomerLedgerRepository customerLedgerRepository;
    private final CustomerRegistrationService customerRegistrationService;
    private final CustomerService customerService;

    public CustomerController(
            CustomerRepository customerRepository,
            CustomerLedgerRepository customerLedgerRepository,
            CustomerRegistrationService customerRegistrationService,
            CustomerService customerService) {
        this.customerRepository = customerRepository;
        this.customerLedgerRepository = customerLedgerRepository;
        this.customerRegistrationService = customerRegistrationService;
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<StandardResponse_CreateCustomer> createCustomer(
            @Valid @RequestBody CreateCustomerRequestDto request,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        Customer saved = customerRegistrationService.registerCustomer(request);

        StandardResponse_CreateCustomer response = new StandardResponse_CreateCustomer(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                new CreateCustomerResponse(saved.getCustomerId())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNoSuchElement(java.util.NoSuchElementException ex) {
        return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", "ERROR");
        body.put("error", message);
        body.put("data", null);
        return ResponseEntity.status(status).body(body);
    }

    public record StandardResponse_Void(
            LocalDateTime timestamp,
            String status,
            String error,
            Void data
    ) {}

    @GetMapping("/{id}")
    public ResponseEntity<StandardResponse_CustomerProfile> getCustomerProfile(
            @PathVariable("id") String id,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        CustomerProfileDTO profile = customerService.getCustomerProfile(id);
        StandardResponse_CustomerProfile response = new StandardResponse_CustomerProfile(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                profile
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<StandardResponse_Void> collectPayment(
            @PathVariable("id") String id,
            @Valid @RequestBody CollectPaymentRequestDto request,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        String agentId = SecurityContextHolder.getContext().getAuthentication().getName();
        customerService.collectPayment(id, request, agentId);

        StandardResponse_Void response = new StandardResponse_Void(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/subscription")
    public ResponseEntity<StandardResponse_CustomerProfile> updateSubscription(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateSubscriptionRequestDto request,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        CustomerProfileDTO profile = customerService.updateSubscription(id, request);
        StandardResponse_CustomerProfile response = new StandardResponse_CustomerProfile(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                profile
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/ledger")
    public ResponseEntity<StandardResponse_LedgerData> getCustomerLedger(
            @PathVariable("id") String id,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        Customer customer = customerRepository.findById(id).orElse(null);
        String fullName = customer != null ? customer.getFullName() : "Unknown";

        List<CustomerLedger> ledgers = customerLedgerRepository.findByCustomer_CustomerId(id);

        List<LedgerItemDTO> items = ledgers.stream()
                .filter(l -> !isFutureMonth(l.getBillingYear(), l.getBillingMonth()))
                .map(l -> new LedgerItemDTO(
                        l.getBillingMonth(),
                        l.getBillingYear(),
                        l.getStatus().name(),
                        l.getPaidAmount(),
                        l.getDueAmount()
                ))
                .collect(Collectors.toList());

        BigDecimal totalBalanceDue = items.stream()
                .map(LedgerItemDTO::dueAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LedgerData ledgerData = new LedgerData(id, fullName, totalBalanceDue, items);
        StandardResponse_LedgerData response = new StandardResponse_LedgerData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                ledgerData
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<StandardResponse_SearchData> searchCustomers(
            @RequestParam("name") String name,
            @RequestParam(value = "block", required = false) String block) {

        List<Customer> customers;
        if (block != null && !block.trim().isEmpty()) {
            customers = customerRepository.findByFullNameContainingIgnoreCaseAndBlockNameContainingIgnoreCase(name, block);
        } else {
            customers = customerRepository.findByFullNameContainingIgnoreCase(name);
        }

        List<SearchItemDTO> suggestions = customers.stream().map(c -> {
            String label = c.getSerialNumber() + ". " + c.getFullName() +
                    (c.getBlockName() != null && !c.getBlockName().isEmpty() ? " (" + c.getBlockName() + ")" : "");
            return new SearchItemDTO(c.getCustomerId(), label);
        }).collect(Collectors.toList());

        StandardResponse_SearchData response = new StandardResponse_SearchData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                suggestions
        );

        return ResponseEntity.ok(response);
    }

    private boolean isFutureMonth(int year, String monthStr) {
        if (year > 2026) return true;
        if (year < 2026) return false;

        List<String> months = List.of("JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
                "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER",
                "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");

        int index = months.indexOf(monthStr.toUpperCase());
        if (index == -1) return false;

        int monthValue = (index % 12) + 1;
        return monthValue > 6; // June 2026 is the limit context
    }
}
