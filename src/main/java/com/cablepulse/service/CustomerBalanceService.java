package com.cablepulse.service;

import com.cablepulse.repository.CustomerLedgerRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.security.SecurityAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerBalanceService {

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

    public static String paymentStatusFromBalance(BigDecimal balanceDue) {
        return balanceDue != null && balanceDue.compareTo(BigDecimal.ZERO) > 0 ? "UNPAID" : "PAID";
    }
}
