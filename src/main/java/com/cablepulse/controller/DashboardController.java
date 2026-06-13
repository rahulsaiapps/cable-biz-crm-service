package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.repository.CustomerLedgerRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.DailyTransactionRepository;
import com.cablepulse.security.SecurityAuth;
import com.cablepulse.security.WorkspaceAuthorizationService;
import com.cablepulse.service.PaymentProcessingService;
import com.cablepulse.util.EtagSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private static final String CURRENCY = "INR";

    private final CustomerRepository customerRepository;
    private final DailyTransactionRepository dailyTransactionRepository;
    private final CustomerLedgerRepository customerLedgerRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public DashboardController(
            CustomerRepository customerRepository,
            DailyTransactionRepository dailyTransactionRepository,
            CustomerLedgerRepository customerLedgerRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService) {
        this.customerRepository = customerRepository;
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.customerLedgerRepository = customerLedgerRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    @GetMapping("/metrics")
    @Transactional(readOnly = true)
    public ResponseEntity<StandardResponse_DashboardData> getDashboardMetrics(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        DashboardData dashboardData = buildDashboardMetrics(SecurityAuth.isOwner());

        return EtagSupport.respondWithEtag(ifNoneMatch, dashboardData, () -> {
            StandardResponse_DashboardData response = new StandardResponse_DashboardData(
                    LocalDateTime.now(),
                    "SUCCESS",
                    null,
                    dashboardData
            );
            return ResponseEntity.ok(response);
        });
    }

    private DashboardData buildDashboardMetrics(boolean includeFinancialSummary) {
        String workspaceId = SecurityAuth.requireWorkspaceId();
        long totalCustomers;
        long pendingCustomers;

        if (SecurityAuth.isOwner()) {
            totalCustomers = customerRepository.countByWorkspaceId(workspaceId);
            pendingCustomers = customerRepository.countPendingCustomersByWorkspaceId(workspaceId);
        } else {
            Set<String> territoryIds = workspaceAuthorizationService.resolveAccessibleTerritoryIds();
            totalCustomers = territoryIds.stream()
                    .mapToLong(customerRepository::countByTerritory_TerritoryId)
                    .sum();
            pendingCustomers = territoryIds.stream()
                    .mapToLong(customerRepository::countPendingCustomersByTerritoryId)
                    .sum();
        }

        CustomerSummary customerSummary = new CustomerSummary(totalCustomers, pendingCustomers);

        FinancialSummary financialSummary = includeFinancialSummary
                ? buildFinancialSummaryForCurrentMonth(workspaceId, YearMonth.now())
                : null;

        return new DashboardData(customerSummary, financialSummary);
    }

    private FinancialSummary buildFinancialSummaryForCurrentMonth(String workspaceId, YearMonth billingMonth) {
        BigDecimal amountPaid = dailyTransactionRepository
                .sumAmountCollectedForCalendarMonth(workspaceId, billingMonth);

        BigDecimal amountPending = customerLedgerRepository.sumDueAmountForBillingPeriod(
                workspaceId,
                billingMonth.getYear(),
                billingMonthNames(billingMonth));
        if (amountPending == null) {
            amountPending = BigDecimal.ZERO;
        }

        String billingCyclePeriod = billingMonth.getMonth().name() + "-" + billingMonth.getYear();
        return new FinancialSummary(amountPaid, amountPending, CURRENCY, billingCyclePeriod);
    }

    private static List<String> billingMonthNames(YearMonth billingMonth) {
        String fullMonth = billingMonth.getMonth().name();
        String shortMonth = PaymentProcessingService.normalizeMonth(fullMonth);
        if (fullMonth.equals(shortMonth)) {
            return List.of(fullMonth);
        }
        return List.of(fullMonth, shortMonth);
    }
}
