package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.CustomerSummary;
import com.cablepulse.dto.DtoClasses.DashboardData;
import com.cablepulse.dto.DtoClasses.FinancialSummary;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.CustomerLedgerRepository;
import com.cablepulse.repository.DailyTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class DashboardService {

    private static final String CURRENCY = "INR";

    private final CustomerRepository customerRepository;
    private final CustomerBalanceService customerBalanceService;
    private final DailyTransactionRepository dailyTransactionRepository;
    private final CustomerLedgerRepository customerLedgerRepository;

    public DashboardService(
            CustomerRepository customerRepository,
            CustomerBalanceService customerBalanceService,
            DailyTransactionRepository dailyTransactionRepository,
            CustomerLedgerRepository customerLedgerRepository) {
        this.customerRepository = customerRepository;
        this.customerBalanceService = customerBalanceService;
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.customerLedgerRepository = customerLedgerRepository;
    }

    @Transactional(readOnly = true)
    public DashboardData getDashboardMetrics(boolean includeFinancialSummary) {
        long totalCustomers = customerRepository.count();
        long pendingCustomers = customerBalanceService.countPendingCustomers();
        CustomerSummary customerSummary = new CustomerSummary(totalCustomers, pendingCustomers);

        FinancialSummary financialSummary = null;
        if (includeFinancialSummary) {
            financialSummary = buildFinancialSummary(LocalDate.now());
        }

        return new DashboardData(customerSummary, financialSummary);
    }

    private FinancialSummary buildFinancialSummary(LocalDate today) {
        YearMonth billingMonth = YearMonth.from(today);
        LocalDateTime monthStart = billingMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = billingMonth.atEndOfMonth().atTime(LocalTime.MAX);

        BigDecimal amountPaid = dailyTransactionRepository.sumAmountCollectedBetween(monthStart, monthEnd);
        if (amountPaid == null) {
            amountPaid = BigDecimal.ZERO;
        }

        List<String> monthNames = billingMonthNames(billingMonth);
        BigDecimal amountPending = customerLedgerRepository.sumDueAmountForBillingPeriod(
                billingMonth.getYear(),
                monthNames);
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
