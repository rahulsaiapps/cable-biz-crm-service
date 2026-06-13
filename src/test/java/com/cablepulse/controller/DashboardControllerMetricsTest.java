package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.DashboardData;
import com.cablepulse.repository.CustomerLedgerRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.DailyTransactionRepository;
import com.cablepulse.security.TenantContext;
import com.cablepulse.security.WorkspaceAuthorizationService;
import com.cablepulse.testsupport.TestWorkspaceSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

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

    @Mock
    private WorkspaceAuthorizationService workspaceAuthorizationService;

    @InjectMocks
    private DashboardController dashboardController;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void buildDashboardMetrics_emptyTables_returnsZeroedFinancialSummary() {
        asOwner();
        TenantContext.set(TestWorkspaceSupport.WORKSPACE_ID, "Test Business");
        when(customerRepository.countByWorkspaceId(TestWorkspaceSupport.WORKSPACE_ID)).thenReturn(0L);
        when(customerRepository.countPendingCustomersByWorkspaceId(TestWorkspaceSupport.WORKSPACE_ID))
                .thenReturn(0L);
        when(dailyTransactionRepository.sumAmountCollectedForCalendarMonth(
                eq(TestWorkspaceSupport.WORKSPACE_ID), any(YearMonth.class)))
                .thenReturn(BigDecimal.ZERO);
        when(customerLedgerRepository.sumDueAmountForBillingPeriod(
                eq(TestWorkspaceSupport.WORKSPACE_ID),
                eq(YearMonth.now().getYear()),
                any()))
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
        asCollector();
        TenantContext.set(TestWorkspaceSupport.WORKSPACE_ID, "Test Business");
        when(workspaceAuthorizationService.resolveAccessibleTerritoryIds()).thenReturn(Set.of("terr-1"));
        when(customerRepository.countByTerritory_TerritoryId("terr-1")).thenReturn(2L);
        when(customerRepository.countPendingCustomersByTerritoryId("terr-1")).thenReturn(1L);

        DashboardData metrics = ReflectionTestUtils.invokeMethod(
                dashboardController,
                "buildDashboardMetrics",
                false);

        assertThat(metrics.customerSummary().totalCustomers()).isEqualTo(2L);
        assertThat(metrics.customerSummary().pendingCustomers()).isEqualTo(1L);
        assertThat(metrics.financialSummary()).isNull();
    }

    @Test
    void buildDashboardMetrics_aggregatesCurrentMonthCollections() {
        asOwner();
        TenantContext.set(TestWorkspaceSupport.WORKSPACE_ID, "Test Business");
        when(customerRepository.countByWorkspaceId(TestWorkspaceSupport.WORKSPACE_ID)).thenReturn(12L);
        when(customerRepository.countPendingCustomersByWorkspaceId(TestWorkspaceSupport.WORKSPACE_ID))
                .thenReturn(3L);
        when(dailyTransactionRepository.sumAmountCollectedForCalendarMonth(
                TestWorkspaceSupport.WORKSPACE_ID, YearMonth.now()))
                .thenReturn(new BigDecimal("12850.00"));
        when(customerLedgerRepository.sumDueAmountForBillingPeriod(
                eq(TestWorkspaceSupport.WORKSPACE_ID),
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

    private static void asOwner() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "owner-uid",
                        "token",
                        List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));
    }

    private static void asCollector() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "collector-uid",
                        "token",
                        List.of(new SimpleGrantedAuthority("ROLE_COLLECTION_BOY"))));
    }
}
