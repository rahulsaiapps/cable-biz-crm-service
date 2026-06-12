package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.DashboardData;
import com.cablepulse.repository.CustomerLedgerRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.DailyTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerMetricsTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private DailyTransactionRepository dailyTransactionRepository;

    @Mock
    private CustomerLedgerRepository customerLedgerRepository;

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    void buildDashboardMetrics_emptyTables_returnsZeroedFinancialSummary() {
        when(customerRepository.count()).thenReturn(0L);
        when(customerRepository.countPendingCustomers()).thenReturn(0L);
        when(dailyTransactionRepository.sumAmountCollectedForCalendarMonth(any(YearMonth.class)))
                .thenReturn(BigDecimal.ZERO);
        when(customerLedgerRepository.sumDueAmountForBillingPeriod(eq(YearMonth.now().getYear()), any()))
                .thenReturn(null);

        DashboardData metrics = ReflectionTestUtils.invokeMethod(
                dashboardController,
                "buildDashboardMetrics",
                true);

        assertThat(metrics.customerSummary().totalCustomers()).isZero();
        assertThat(metrics.customerSummary().pendingCustomers()).isZero();
        assertThat(metrics.financialSummary()).isNotNull();
        assertThat(metrics.financialSummary().amountPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.financialSummary().amountPending()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.financialSummary().currency()).isEqualTo("INR");
    }

    @Test
    void buildDashboardMetrics_collectionBoyRole_hidesFinancialSummary() {
        when(customerRepository.count()).thenReturn(0L);
        when(customerRepository.countPendingCustomers()).thenReturn(0L);

        DashboardData metrics = ReflectionTestUtils.invokeMethod(
                dashboardController,
                "buildDashboardMetrics",
                false);

        assertThat(metrics.financialSummary()).isNull();
    }

    @Test
    void buildDashboardMetrics_aggregatesCurrentMonthCollections() {
        when(customerRepository.count()).thenReturn(12L);
        when(customerRepository.countPendingCustomers()).thenReturn(3L);
        when(dailyTransactionRepository.sumAmountCollectedForCalendarMonth(YearMonth.now()))
                .thenReturn(new BigDecimal("12850.00"));
        when(customerLedgerRepository.sumDueAmountForBillingPeriod(
                eq(YearMonth.now().getYear()),
                any(List.class)))
                .thenReturn(new BigDecimal("4200.00"));

        DashboardData metrics = ReflectionTestUtils.invokeMethod(
                dashboardController,
                "buildDashboardMetrics",
                true);

        assertThat(metrics.customerSummary().totalCustomers()).isEqualTo(12L);
        assertThat(metrics.customerSummary().pendingCustomers()).isEqualTo(3L);
        assertThat(metrics.financialSummary().amountPaid()).isEqualByComparingTo("12850.00");
        assertThat(metrics.financialSummary().amountPending()).isEqualByComparingTo("4200.00");
    }
}
