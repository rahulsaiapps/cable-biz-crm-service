package com.cablepulse.service;

import com.cablepulse.repository.CustomerLedgerRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.security.SecurityAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerBalanceService {

    public record CurrentMonthLedger(BigDecimal paidAmount, BigDecimal dueAmount) {}

    public record CustomerPaymentSummary(BigDecimal balanceDue, String paymentStatus) {}

    private final CustomerLedgerRepository customerLedgerRepository;
    private final CustomerRepository customerRepository;

    public CustomerBalanceService(
            CustomerLedgerRepository customerLedgerRepository,
            CustomerRepository customerRepository) {
        this.customerLedgerRepository = customerLedgerRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> sumDueAmountByCustomerIds(Collection<String> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = customerLedgerRepository.sumDueAmountGroupedByCustomerId(customerIds);
        Map<String, BigDecimal> balances = new HashMap<>();
        for (Object[] row : rows) {
            balances.put((String) row[0], (BigDecimal) row[1]);
        }
        return balances;
    }

    @Transactional(readOnly = true)
    public BigDecimal sumTotalDueForCustomer(String customerId) {
        return customerLedgerRepository.sumDueAmountForCustomer(customerId);
    }

    @Transactional(readOnly = true)
    public long countPendingCustomers() {
        return customerRepository.countPendingCustomersByWorkspaceId(SecurityAuth.requireWorkspaceId());
    }

    @Transactional(readOnly = true)
    public Map<String, CurrentMonthLedger> currentMonthLedgerByCustomerIds(Collection<String> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return Map.of();
        }
        YearMonth billingPeriod = YearMonth.now();
        String month = PaymentProcessingService.normalizeMonth(
                billingPeriod.getMonth().name());
        List<Object[]> rows = customerLedgerRepository.findCurrentMonthLedgerByCustomerIds(
                customerIds,
                billingPeriod.getYear(),
                month);
        Map<String, CurrentMonthLedger> byCustomer = new HashMap<>();
        for (Object[] row : rows) {
            byCustomer.put(
                    (String) row[0],
                    new CurrentMonthLedger((BigDecimal) row[1], (BigDecimal) row[2]));
        }
        return byCustomer;
    }

    public CustomerPaymentSummary summarizeCustomerPayment(
            boolean hasAnyLedgerRow,
            BigDecimal monthlyRate,
            BigDecimal totalBalanceDue,
            CurrentMonthLedger currentMonthLedger) {
        BigDecimal balanceDue = totalBalanceDue != null ? totalBalanceDue : BigDecimal.ZERO;
        if (!hasAnyLedgerRow) {
            if (monthlyRate != null && monthlyRate.compareTo(BigDecimal.ZERO) > 0) {
                return new CustomerPaymentSummary(monthlyRate, "UNPAID");
            }
            return new CustomerPaymentSummary(BigDecimal.ZERO, "PAID");
        }

        BigDecimal currentPaid = currentMonthLedger != null ? currentMonthLedger.paidAmount() : BigDecimal.ZERO;
        BigDecimal currentDue = currentMonthLedger != null ? currentMonthLedger.dueAmount() : BigDecimal.ZERO;
        String paymentStatus = paymentStatusFromLedger(balanceDue, currentPaid, currentDue);
        return new CustomerPaymentSummary(balanceDue, paymentStatus);
    }

    public static String paymentStatusFromBalance(BigDecimal balanceDue) {
        return balanceDue != null && balanceDue.compareTo(BigDecimal.ZERO) > 0 ? "UNPAID" : "PAID";
    }

    public static String paymentStatusFromLedger(
            BigDecimal totalBalanceDue,
            BigDecimal currentMonthPaid,
            BigDecimal currentMonthDue) {
        if (totalBalanceDue == null || totalBalanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            return "PAID";
        }
        if (currentMonthPaid != null
                && currentMonthPaid.compareTo(BigDecimal.ZERO) > 0
                && currentMonthDue != null
                && currentMonthDue.compareTo(BigDecimal.ZERO) > 0) {
            return "PARTIAL";
        }
        return "UNPAID";
    }
}
