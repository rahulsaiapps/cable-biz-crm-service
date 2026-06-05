package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.repository.CustomerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final CustomerRepository customerRepository;

    public DashboardController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/metrics")
    public ResponseEntity<StandardResponse_DashboardData> getDashboardMetrics(
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        long totalCustomers = customerRepository.count();
        long pendingCustomers = 0; // Mock calculation or custom query

        CustomerSummary customerSummary = new CustomerSummary(totalCustomers, pendingCustomers);

        // RBAC Privacy Rule
        boolean isCollectionBoy = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COLLECTION_BOY"));

        FinancialSummary financialSummary = null;
        if (!isCollectionBoy) {
            financialSummary = new FinancialSummary(
                    BigDecimal.valueOf(15450.00),
                    BigDecimal.valueOf(3200.00),
                    "INR",
                    "JUNE-2026"
            );
        }

        DashboardData dashboardData = new DashboardData(customerSummary, financialSummary);
        StandardResponse_DashboardData response = new StandardResponse_DashboardData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                dashboardData
        );

        return ResponseEntity.ok(response);
    }
}
