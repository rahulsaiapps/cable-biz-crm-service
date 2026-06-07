package com.cablepulse.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DtoClasses {

    public record CustomerSummary(
        long totalCustomers,
        long pendingCustomers
    ) {}

    public record FinancialSummary(
        BigDecimal amountPaid,
        BigDecimal amountPending,
        String currency,
        String billingCyclePeriod
    ) {}

    public record DashboardData(
        CustomerSummary customerSummary,
        FinancialSummary financialSummary
    ) {}

    public record StandardResponse_DashboardData(
        LocalDateTime timestamp,
        String status,
        String error,
        DashboardData data
    ) {}

    public record WorkspaceCustomerDTO(
        String customerId,
        int serialNumber,
        String fullName,
        String doorNumber,
        String streetName,
        String activePlanName,
        BigDecimal monthlyRate,
        String paymentStatus,
        BigDecimal balanceDue,
        String connectionType,
        String boxNumber,
        String cardNumber
    ) {}

    public record WorkspaceData(
        String locationId,
        String locationName,
        List<WorkspaceCustomerDTO> customers
    ) {}

    public record StandardResponse_WorkspaceData(
        LocalDateTime timestamp,
        String status,
        String error,
        WorkspaceData data
    ) {}

    public record LedgerItemDTO(
        String month,
        int year,
        String status,
        BigDecimal paidAmount,
        BigDecimal dueAmount
    ) {}

    public record LedgerData(
        String customerId,
        String fullName,
        BigDecimal totalBalanceDue,
        List<LedgerItemDTO> ledger
    ) {}

    public record StandardResponse_LedgerData(
        LocalDateTime timestamp,
        String status,
        String error,
        LedgerData data
    ) {}

    public record SearchItemDTO(
        String customerId,
        String label
    ) {}

    public record StandardResponse_SearchData(
        LocalDateTime timestamp,
        String status,
        String error,
        List<SearchItemDTO> data
    ) {}

    public record SyncEventDTO(
        String eventId,
        String actionType,
        UUID idempotencyToken,
        Map<String, Object> payload
    ) {}

    public record SyncRequestPayload(
        UUID syncBatchId,
        List<SyncEventDTO> events
    ) {}

    public record SyncResolutionDTO(
        List<String> processedEventIds,
        List<String> rejectedEventIds
    ) {}

    public record SyncResolutionInner(
        SyncResolutionDTO syncResolution
    ) {}

    public record StandardResponse_SyncResolution(
        LocalDateTime timestamp,
        String status,
        String error,
        SyncResolutionInner data
    ) {}

    public record ExpenseCreatedData(Long expenseId) {}

    public record StandardResponse_ExpenseCreated(
        LocalDateTime timestamp,
        String status,
        String error,
        ExpenseCreatedData data
    ) {}

    public record SettlementCreatedData(Long settlementId) {}

    public record StandardResponse_SettlementCreated(
        LocalDateTime timestamp,
        String status,
        String error,
        SettlementCreatedData data
    ) {}

    public record DailyCashSummary(
        Double totalCollectedToday,
        Double totalExpensedToday,
        Double totalIspSettlementsToday,
        Double netCashInHand
    ) {}

    public record StandardResponse_DailyCashSummaryData(
        LocalDateTime timestamp,
        String status,
        String error,
        DailyCashSummary data
    ) {}

    public record PlanItemDTO(
        String planId,
        String name,
        BigDecimal price,
        String details
    ) {}

    public record StandardResponse_PlansData(
        LocalDateTime timestamp,
        String status,
        String error,
        List<PlanItemDTO> data
    ) {}
}
