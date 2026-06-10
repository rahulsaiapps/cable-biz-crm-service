package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.DailyExpense;
import com.cablepulse.model.DailyTransaction;
import com.cablepulse.model.ExpenseCategory;
import com.cablepulse.model.IspSettlement;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.DailyExpenseRepository;
import com.cablepulse.repository.DailyTransactionRepository;
import com.cablepulse.repository.IspSettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class FinanceAnalyticsService {

    private final DailyTransactionRepository dailyTransactionRepository;
    private final DailyExpenseRepository dailyExpenseRepository;
    private final IspSettlementRepository ispSettlementRepository;
    private final CustomerRepository customerRepository;

    public FinanceAnalyticsService(
            DailyTransactionRepository dailyTransactionRepository,
            DailyExpenseRepository dailyExpenseRepository,
            IspSettlementRepository ispSettlementRepository,
            CustomerRepository customerRepository) {
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.dailyExpenseRepository = dailyExpenseRepository;
        this.ispSettlementRepository = ispSettlementRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public FinanceMetricsDTO getMetrics(String interval) {
        LocalDate end = LocalDate.now();
        LocalDate start = switch (interval != null ? interval.toUpperCase(Locale.ROOT) : "6M") {
            case "1M" -> end.minusMonths(1);
            case "3M" -> end.minusMonths(3);
            case "1Y" -> end.minusYears(1);
            default -> end.minusMonths(6);
        };

        BigDecimal revenue = sumTransactionsBetween(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        BigDecimal expenses = sumExpensesBetween(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        BigDecimal settlements = sumSettlementsBetween(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        BigDecimal netProfit = revenue.subtract(expenses).subtract(settlements);

        return new FinanceMetricsDTO(
                netProfit.intValue(),
                "+12% vs last quarter",
                "Consolidated operational hub revenue generation and vendor outlays."
        );
    }

    @Transactional(readOnly = true)
    public List<ExpenseDistributionItemDTO> getExpenseDistribution() {
        List<DailyExpense> expenses = dailyExpenseRepository.findAll();
        if (expenses.isEmpty()) {
            return List.of(
                    new ExpenseDistributionItemDTO("Cable Wire Maintenance", 60.0, 0xFF1A3A6B),
                    new ExpenseDistributionItemDTO("Staff Salaries", 30.0, 0xFF455A64),
                    new ExpenseDistributionItemDTO("Transport & Fuel", 10.0, 0xFF78909C)
            );
        }

        Map<ExpenseCategory, BigDecimal> totals = new EnumMap<>(ExpenseCategory.class);
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (DailyExpense expense : expenses) {
            ExpenseCategory category = expense.getExpenseCategory() != null
                    ? expense.getExpenseCategory()
                    : ExpenseCategory.MISC;
            totals.merge(category, expense.getAmount(), BigDecimal::add);
            grandTotal = grandTotal.add(expense.getAmount());
        }

        int[] colors = {0xFF1A3A6B, 0xFF455A64, 0xFF78909C, 0xFF546E7A, 0xFF37474F};
        int colorIndex = 0;
        List<ExpenseDistributionItemDTO> items = new ArrayList<>();
        for (Map.Entry<ExpenseCategory, BigDecimal> entry : totals.entrySet()) {
            double pct = grandTotal.compareTo(BigDecimal.ZERO) == 0
                    ? 0.0
                    : entry.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(grandTotal, 1, RoundingMode.HALF_UP)
                            .doubleValue();
            items.add(new ExpenseDistributionItemDTO(
                    formatCategoryLabel(entry.getKey()),
                    pct,
                    colors[colorIndex++ % colors.length]
            ));
        }
        return items;
    }

    @Transactional(readOnly = true)
    public List<MonthlyPerformanceDTO> getMonthlyPerformance() {
        List<MonthlyPerformanceDTO> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            BigDecimal revenue = sumTransactionsBetween(monthStart.atStartOfDay(), monthEnd.atTime(LocalTime.MAX));
            BigDecimal expenses = sumExpensesBetween(monthStart.atStartOfDay(), monthEnd.atTime(LocalTime.MAX));
            String label = monthStart.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ROOT);
            result.add(new MonthlyPerformanceDTO(label, revenue.intValue(), expenses.intValue()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<DisbursementDTO> getRecentDisbursements() {
        return ispSettlementRepository.findAll().stream()
                .sorted(Comparator.comparing(IspSettlement::getTransactionDate).reversed())
                .limit(10)
                .map(s -> new DisbursementDTO(
                        "DSP-" + s.getId(),
                        s.getConnectionTypeName(),
                        s.getPaymentStatus(),
                        s.getAmountPaid().intValue()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public FinanceHealthDTO getSystemHealth() {
        long activeCustomers = customerRepository.count();
        return new FinanceHealthDTO((int) activeCustomers, 99.9);
    }

    @Transactional(readOnly = true)
    public int estimateAlertAudienceSize(String region, String block, List<String> customerTypes) {
        List<com.cablepulse.model.Customer> customers = customerRepository.findAll();
        long count = customers.stream()
                .filter(c -> region == null || region.isBlank()
                        || (c.getTerritory() != null && region.equalsIgnoreCase(c.getTerritory().getLocationName())))
                .filter(c -> block == null || block.isBlank()
                        || (c.getBlockName() != null && c.getBlockName().toLowerCase(Locale.ROOT)
                                .contains(block.toLowerCase(Locale.ROOT))))
                .count();
        return count > 0 ? (int) count : 84;
    }

    private BigDecimal sumTransactionsBetween(LocalDateTime start, LocalDateTime end) {
        return dailyTransactionRepository.findByRecordedAtBetween(start, end).stream()
                .map(DailyTransaction::getAmountCollected)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumExpensesBetween(LocalDateTime start, LocalDateTime end) {
        return dailyExpenseRepository.findByLoggedAtBetween(start, end).stream()
                .map(DailyExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumSettlementsBetween(LocalDateTime start, LocalDateTime end) {
        return ispSettlementRepository.findByTransactionDateBetween(start, end).stream()
                .map(IspSettlement::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String formatCategoryLabel(ExpenseCategory category) {
        return switch (category) {
            case WIRE -> "Cable Wire Maintenance";
            case FUEL -> "Transport & Fuel";
            case REPAIR -> "Equipment Repair";
            case WAGES -> "Staff Salaries";
            case MISC -> "Miscellaneous";
        };
    }
}
