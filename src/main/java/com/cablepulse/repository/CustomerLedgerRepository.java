package com.cablepulse.repository;

import com.cablepulse.model.CustomerLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerLedgerRepository extends JpaRepository<CustomerLedger, Long> {

    List<CustomerLedger> findByCustomer_CustomerId(String customerId);

    List<CustomerLedger> findByCustomer_CustomerIdAndBillingYearOrderByUpdatedAtDesc(String customerId, int billingYear);
}
