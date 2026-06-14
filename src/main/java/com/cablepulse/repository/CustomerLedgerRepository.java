package com.cablepulse.repository;

import com.cablepulse.model.CustomerLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface CustomerLedgerRepository extends JpaRepository<CustomerLedger, Long> {

    List<CustomerLedger> findByCustomer_CustomerId(String customerId);

    boolean existsByCustomer_CustomerId(String customerId);

    List<CustomerLedger> findByCustomer_CustomerIdAndBillingYearOrderByUpdatedAtDesc(String customerId, int billingYear);

    @Query("""
            SELECT l.customer.customerId, COALESCE(SUM(l.dueAmount), 0)
            FROM CustomerLedger l
            WHERE l.customer.customerId IN :customerIds
            GROUP BY l.customer.customerId
            """)
    List<Object[]> sumDueAmountGroupedByCustomerId(@Param("customerIds") Collection<String> customerIds);

    @Query("""
            SELECT COALESCE(SUM(l.dueAmount), 0)
            FROM CustomerLedger l
            WHERE l.customer.customerId = :customerId
            """)
    BigDecimal sumDueAmountForCustomer(@Param("customerId") String customerId);

    @Query("""
            SELECT COALESCE(SUM(l.dueAmount), 0)
            FROM CustomerLedger l
            WHERE l.billingYear = :year
              AND UPPER(l.billingMonth) IN :monthNames
              AND l.dueAmount > 0
              AND l.customer.workspaceId = :workspaceId
            """)
    BigDecimal sumDueAmountForBillingPeriod(
            @Param("workspaceId") String workspaceId,
            @Param("year") int year,
            @Param("monthNames") Collection<String> monthNames);
}
