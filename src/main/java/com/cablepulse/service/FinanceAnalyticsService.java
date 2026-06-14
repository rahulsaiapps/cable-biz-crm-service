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
import com.cablepulse.security.SecurityAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class FinanceAnalyticsService {

    private static final int[] EXPENSE_COLORS = {
            0xFF6366F1, 0xFF8B5CF6, 0xFFEC4899, 0xFF14B8A6, 0xFFF59E0B, 0xFF3B82F6
    };

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
    public FinanceMetricsDTO getMetrics(String interval, LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : resolveIntervalStart(interval, end);

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("startDate must be on or before endDate");
        }

        BigDecimal netProfit = netProfitBetween(start, end);
        long periodDays = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate prevEnd = start.minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(periodDays - 1);
        BigDecimal previousNetProfit = netProfitBetween(prevStart, prevEnd);

        return new FinanceMetricsDTO(
                netProfit.intValue(),
                formatTrend(netProfit, previousNetProfit, periodDays),
                ""
        );
    }

    @Transactional(readOnly = true)
    public List<ExpenseDistributionItemDTO> getExpenseDistribution(LocalDate startDate, LocalDate endDate) {
        String workspaceId = SecurityAuth.requireWorkspaceId();
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusMonths(6);

        List<DailyExpense> expenses = dailyExpenseRepository.findByWorkspaceIdAndLoggedAtBetween(
                workspaceId, start.atStartOfDay(), end.atTime(LocalTime.MAX));
        if (expenses.isEmpty()) {
            return List.of();
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
                    EXPENSE_COLORS[colorIndex++ % EXPENSE_COLORS.length]
            ));
        }
        items.sort(Comparator.comparingDouble(ExpenseDistributionItemDTO::percentage).reversed());
        return items;
    }

    @Transactional(readOnly = true)
    public List<MonthlyPerformanceDTO> getMonthlyPerformance(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusMonths(5).withDayOfMonth(1);

        YearMonth cursor = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        List<MonthlyPerformanceDTO> result = new ArrayList<>();

        while (!cursor.isAfter(last) && result.size() < 24) {
            LocalDate monthStart = cursor.atDay(1);
            LocalDate monthEnd = cursor.atEndOfMonth();
            if (monthEnd.isBefore(start)) {
                cursor = cursor.plusMonths(1);
                continue;
            }
            LocalDate effectiveStart = monthStart.isBefore(start) ? start : monthStart;
            LocalDate effectiveEnd = monthEnd.isAfter(end) ? end : monthEnd;

            BigDecimal revenue = sumTransactionsBetween(
                    effectiveStart.atStartOfDay(), effectiveEnd.atTime(LocalTime.MAX));
            BigDecimal expenses = sumExpensesBetween(
                    effectiveStart.atStartOfDay(), effectiveEnd.atTime(LocalTime.MAX));
            String label = cursor.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    .toUpperCase(Locale.ROOT);
            result.add(new MonthlyPerformanceDTO(label, revenue.intValue(), expenses.intValue()));
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<DisbursementDTO> getRecentDisbursements() {
        String workspaceId = SecurityAuth.requireWorkspaceId();
        LocalDateTime start = LocalDate.now().minusYears(2).atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        return ispSettlementRepository.findByWorkspaceIdAndTransactionDateBetween(workspaceId, start, end).stream()
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
        long activeCustomers = customerRepository.countByWorkspaceId(SecurityAuth.requireWorkspaceId());
        return new FinanceHealthDTO((int) activeCustomers, 0.0);
    }

    @Transactional(readOnly = true)
    public int estimateAlertAudienceSize(String region, String block, List<String> customerTypes) {
        List<com.cablepulse.model.Customer> customers =
                customerRepository.findByWorkspaceId(SecurityAuth.requireWorkspaceId());
        long count = customers.stream()
                .filter(c -> region == null || region.isBlank()
                        || (c.getTerritory() != null && region.equalsIgnoreCase(c.getTerritory().getLocationName())))
                .filter(c -> block == null || block.isBlank()
                        || (c.getBlockName() != null && c.getBlockName().toLowerCase(Locale.ROOT)
                                .contains(block.toLowerCase(Locale.ROOT))))
                .count();
        return (int) count;
    }

    private BigDecimal netProfitBetween(LocalDate start, LocalDate end) {
        BigDecimal revenue = sumTransactionsBetween(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        BigDecimal expenses = sumExpensesBetween(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        BigDecimal settlements = sumSettlementsBetween(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        return revenue.subtract(expenses).subtract(settlements);
    }

    private static LocalDate resolveIntervalStart(String interval, LocalDate end) {
        return switch (interval != null ? interval.toUpperCase(Locale.ROOT) : "6M") {
            case "1M" -> end.minusMonths(1);
            case "3M" -> end.minusMonths(3);
            case "1Y" -> end.minusYears(1);
            default -> end.minusMonths(6);
        };
    }

    private static String formatTrend(BigDecimal current, BigDecimal previous, long periodDays) {
        if (current.compareTo(BigDecimal.ZERO) == 0 && previous.compareTo(BigDecimal.ZERO) == 0) {
            return "";
        }
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? "Up from prior period" : "";
        }

        BigDecimal change = current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 0, RoundingMode.HALF_UP);
        String periodLabel = periodDays <= 31 ? "last month" : "prior period";
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + change.intValue() + "% vs " + periodLabel;
        }
        if (change.compareTo(BigDecimal.ZERO) < 0) {
            return change.intValue() + "% vs " + periodLabel;
        }
        return "Flat vs " + periodLabel;
    }

    private BigDecimal sumTransactionsBetween(LocalDateTime start, LocalDateTime end) {
        return dailyTransactionRepository
                .findByWorkspaceIdAndRecordedAtBetween(SecurityAuth.requireWorkspaceId(), start, end).stream()
                .map(DailyTransaction::getAmountCollected)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumExpensesBetween(LocalDateTime start, LocalDateTime end) {
        return dailyExpenseRepository
                .findByWorkspaceIdAndLoggedAtBetween(SecurityAuth.requireWorkspaceId(), start, end).stream()
                .map(DailyExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumSettlementsBetween(LocalDateTime start, LocalDateTime end) {
        return ispSettlementRepository
                .findByWorkspaceIdAndTransactionDateBetween(SecurityAuth.requireWorkspaceId(), start, end).stream()
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
