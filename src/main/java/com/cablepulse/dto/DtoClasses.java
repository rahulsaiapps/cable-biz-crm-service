package com.cablepulse.dto;

import com.cablepulse.model.EmployeeRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
        BigDecimal totalCollectedToday,
        BigDecimal totalExpensedToday,
        BigDecimal totalIspSettlementsToday,
        BigDecimal netCashInHand
    ) {}

    public record StandardResponse_DailyCashSummaryData(
        LocalDateTime timestamp,
        String status,
        String error,
        DailyCashSummary data
    ) {}

    public record PlanItemDTO(
        @JsonProperty("id") String planId,
        String name,
        BigDecimal price,
        @JsonProperty("channels_text") String details
    ) {}

    public record CreatePlanRequestDto(
        @NotBlank String name,
        @NotNull @Positive Integer price,
        @JsonProperty("channels_text") String channelsText,
        @JsonProperty("is_hd") Boolean isHd,
        @NotBlank String provider
    ) {}

    public record PlanCreatedData(
        @JsonProperty("createdPlanId") String createdPlanId
    ) {}

    public record StandardResponse_PlanCreated(
        LocalDateTime timestamp,
        String status,
        String error,
        PlanCreatedData data
    ) {}

    public record StandardResponse_PlansData(
        LocalDateTime timestamp,
        String status,
        String error,
        List<PlanItemDTO> data
    ) {}

    public record SaasTierDTO(
        String tierName,
        String billingCycle,
        Double retailPrice,
        Double discountedPrice
    ) {}

    public record SaasPricingData(
        boolean promotionalTrialActive,
        LocalDateTime trialEndsAt,
        String currencyCode,
        List<SaasTierDTO> tiers
    ) {}

    public record StandardResponse_SaasPricingData(
        LocalDateTime timestamp,
        String status,
        String error,
        SaasPricingData data
    ) {}

    public record UpgradeIntentRequestDto(
        @NotBlank String tierName,
        @NotBlank String billingCycle,
        @NotNull @Positive Double amount
    ) {}

    public record UpgradeIntentData(Long id) {}

    public record StandardResponse_UpgradeIntentData(
        LocalDateTime timestamp,
        String status,
        String error,
        UpgradeIntentData data
    ) {}

    public record CreateEmployeeRequestDto(
        @NotBlank String fullName,
        @NotNull EmployeeRole role,
        String email
    ) {}

    public record EmployeeDTO(
        String employeeId,
        String fullName,
        EmployeeRole role
    ) {}

    public record UpdateProfileRequestDto(
        @JsonProperty("full_name") String fullName,
        String email,
        String description
    ) {}

    public record EmployeeProfileDTO(
        @JsonProperty("employee_id") String employeeId,
        @JsonProperty("full_name") String fullName,
        String email,
        String description,
        EmployeeRole role
    ) {}

    public record StandardResponse_EmployeeProfileData(
        LocalDateTime timestamp,
        String status,
        String error,
        EmployeeProfileDTO data
    ) {}

    public record StandardResponse_EmployeeData(
        LocalDateTime timestamp,
        String status,
        String error,
        EmployeeDTO data
    ) {}

    public record StandardResponse_LocationNames(
        LocalDateTime timestamp,
        String status,
        String error,
        List<String> data
    ) {}

    public record TerritorySummaryDTO(
        @JsonProperty("territory_id") String territoryId,
        @JsonProperty("location_name") String locationName,
        @JsonProperty("customer_count") long customerCount,
        @JsonProperty("active_count") long activeCount,
        @JsonProperty("pending_count") long pendingCount
    ) {}

    public record StandardResponse_Territories(
        LocalDateTime timestamp,
        String status,
        String error,
        List<TerritorySummaryDTO> data
    ) {}

    public record StandardResponse_Territory(
        LocalDateTime timestamp,
        String status,
        String error,
        TerritorySummaryDTO data
    ) {}

    public record CreateCustomerResponse(
        @JsonProperty("newCustomerId") String newCustomerId
    ) {}

    public record StandardResponse_CreateCustomer(
        LocalDateTime timestamp,
        String status,
        String error,
        CreateCustomerResponse data
    ) {}
}
