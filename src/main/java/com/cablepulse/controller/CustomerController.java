package com.cablepulse.controller;

import com.cablepulse.dto.CreateCustomerRequestDto;
import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.Customer;
import com.cablepulse.model.CustomerLedger;
import com.cablepulse.repository.CustomerLedgerRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.service.CustomerRegistrationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CustomerLedgerRepository customerLedgerRepository;
    private final CustomerRegistrationService customerRegistrationService;

    public CustomerController(
            CustomerRepository customerRepository,
            CustomerLedgerRepository customerLedgerRepository,
            CustomerRegistrationService customerRegistrationService) {
        this.customerRepository = customerRepository;
        this.customerLedgerRepository = customerLedgerRepository;
        this.customerRegistrationService = customerRegistrationService;
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
    public ResponseEntity<StandardResponse_CreateCustomer> handleTerritoryNotFound(
            EntityNotFoundException ex) {
        StandardResponse_CreateCustomer response = new StandardResponse_CreateCustomer(
                LocalDateTime.now(),
                "ERROR",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
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
