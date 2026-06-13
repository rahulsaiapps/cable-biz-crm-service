package com.cablepulse.dto;

import com.cablepulse.model.EmployeeRole;
import com.fasterxml.jackson.annotation.JsonAlias;
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
        @JsonProperty("channels_text") String details,
        String provider
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
        @NotBlank @JsonAlias("tier_name") String tierName,
        @NotBlank @JsonAlias("billing_cycle") String billingCycle,
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
        @NotBlank @JsonProperty("full_name") String fullName,
        @NotNull EmployeeRole role,
        String email,
        @JsonProperty("assigned_villages") List<String> assignedVillages
    ) {}

    public record EmployeeDTO(
        @JsonProperty("employee_id") String employeeId,
        @JsonProperty("full_name") String fullName,
        EmployeeRole role,
        String email,
        @JsonProperty("assigned_villages") List<String> assignedVillages,
        @JsonProperty("today_collection") long todayCollection,
        @JsonProperty("phone_number") String phoneNumber
    ) {
        public static EmployeeDTO fromEntity(com.cablepulse.model.Employee employee) {
            return new EmployeeDTO(
                    employee.getEmployeeId(),
                    employee.getFullName(),
                    employee.getRole(),
                    employee.getEmail(),
                    employee.getAssignedVillages() != null
                            ? List.copyOf(employee.getAssignedVillages())
                            : List.of(),
                    0L,
                    null
            );
        }
    }

    public record StandardResponse_EmployeeList(
        LocalDateTime timestamp,
        String status,
        String error,
        List<EmployeeDTO> data
    ) {}

    public record UpdateProfileRequestDto(
        @JsonProperty("full_name") String fullName,
        String email,
        String description
    ) {}

    public record UpdateEmployeeRequestDto(
        @JsonProperty("full_name") String fullName,
        String email,
        @JsonProperty("assigned_villages") List<String> assignedVillages
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

    public record EmployeeActivityEntryDTO(
        @JsonProperty("entry_type") String entryType,
        String title,
        String subtitle,
        java.math.BigDecimal amount,
        @JsonProperty("recorded_at") java.time.LocalDateTime recordedAt
    ) {}

    public record StandardResponse_EmployeeActivityList(
        LocalDateTime timestamp,
        String status,
        String error,
        List<EmployeeActivityEntryDTO> data
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

    public record CustomerProfileDTO(
        String customerId,
        String fullName,
        String doorNumber,
        String streetName,
        @JsonProperty("territory_id") String territoryId,
        @JsonProperty("territory_name") String territoryName,
        String activePlanName,
        BigDecimal monthlyRate,
        String paymentStatus,
        BigDecimal balanceDue,
        @JsonProperty("phone_number") String phoneNumber
    ) {}

    public record StandardResponse_CustomerProfile(
        LocalDateTime timestamp,
        String status,
        String error,
        CustomerProfileDTO data
    ) {}

    public record CollectPaymentRequestDto(
        @NotNull @Positive Integer amount,
        @JsonAlias({"monthsPaid", "months_paid"}) List<String> monthsPaid,
        Integer year,
        @JsonProperty("payment_mode") String paymentMode,
        @JsonProperty("transaction_ref") String transactionRef
    ) {
        public List<String> resolvedMonths() {
            return monthsPaid != null ? monthsPaid : List.of();
        }
    }

    public record UpdateSubscriptionRequestDto(
        @JsonProperty("plan_name") String planName,
        @JsonProperty("plan_monthly_rate") Integer planMonthlyRate
    ) {
        public String resolvedPlanName() {
            return planName != null && !planName.isBlank() ? planName.trim() : "Custom Plan";
        }

        public int resolvedMonthlyRate() {
            return planMonthlyRate != null ? planMonthlyRate : 0;
        }
    }

    public record DailyLedgerTransactionDTO(
        @JsonProperty("transactionId") String transactionId,
        String customerName,
        @JsonProperty("blockLocation") String blockLocation,
        String timestamp,
        BigDecimal amountCollected,
        String paymentMode,
        @JsonProperty("fieldAgentName") String fieldAgentName,
        boolean isExpense,
        String expenseCategory,
        boolean isIspSettlement,
        String paymentStatus
    ) {}

    public record DailyLedgerSummaryDTO(
        BigDecimal collectedAmountToday,
        int totalSettledHomesCount
    ) {}

    public record DailyLedgerBookData(
        DailyLedgerSummaryDTO summary,
        List<DailyLedgerTransactionDTO> transactions
    ) {}

    public record StandardResponse_DailyLedgerBook(
        LocalDateTime timestamp,
        String status,
        String error,
        DailyLedgerBookData data
    ) {}

    public record RecordDailyTransactionRequestDto(
        @JsonProperty("customer_id") String customerId,
        @JsonProperty("customer_name") String customerName,
        @JsonProperty("block_code") String blockCode,
        @JsonProperty("amount_collected") Integer amountCollected,
        @JsonProperty("payment_type") String paymentType,
        @JsonProperty("collected_by") String collectedBy,
        String date
    ) {}

    public record FinanceMetricsDTO(
        @JsonProperty("net_profit") int netProfit,
        @JsonProperty("trend_text") String trendText,
        String description
    ) {}

    public record ExpenseDistributionItemDTO(
        String label,
        double percentage,
        @JsonProperty("color_hex") int colorHex
    ) {}

    public record MonthlyPerformanceDTO(
        String month,
        int revenue,
        int expenses
    ) {}

    public record DisbursementDTO(
        String reference,
        String vendor,
        String status,
        int amount
    ) {}

    public record FinanceHealthDTO(
        @JsonProperty("active_subscriptions") int activeSubscriptions,
        @JsonProperty("uptime_percentage") double uptimePercentage
    ) {}

    public record AlertTargetSizeDTO(
        @JsonProperty("target_size") int targetSize
    ) {}

    public record BroadcastAcceptedData(
        String trackingId
    ) {}

    public record StandardResponse_BroadcastAccepted(
        LocalDateTime timestamp,
        String status,
        String error,
        BroadcastAcceptedData data
    ) {}

    public record NotificationDispatchData(
        @JsonProperty("notificationTrackingId") String notificationTrackingId,
        @JsonProperty("estimatedCreditsUsed") int estimatedCreditsUsed
    ) {}

    public record StandardResponse_NotificationDispatch(
        LocalDateTime timestamp,
        String status,
        String error,
        NotificationDispatchData data
    ) {}
}
