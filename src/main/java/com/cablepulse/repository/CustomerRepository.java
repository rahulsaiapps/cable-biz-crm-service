package com.cablepulse.repository;

import com.cablepulse.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    @Query("""
            SELECT c FROM Customer c
            LEFT JOIN FETCH c.globalPlan
            WHERE c.territory.territoryId = :territoryId
            """)
    List<Customer> findByTerritoryIdWithPlan(@Param("territoryId") String territoryId);

    @Query("""
            SELECT COUNT(DISTINCT c.customerId) FROM Customer c
            WHERE (c.globalPlan IS NULL AND c.customRateOverride IS NULL)
               OR EXISTS (
                   SELECT 1 FROM CustomerLedger l
                   WHERE l.customer.customerId = c.customerId
                     AND l.dueAmount > 0
               )
            """)
    long countPendingActivationOrOutstanding();

    List<Customer> findByTerritory_TerritoryId(String territoryId);

    List<Customer> findByTerritory_TerritoryIdAndBlockName(String territoryId, String blockName);

    long countByTerritory_TerritoryId(String territoryId);

    @Query("""
            SELECT COALESCE(MAX(c.serialNumber), 0) FROM Customer c
            WHERE c.territory.territoryId = :territoryId
            """)
    int findMaxSerialNumberByTerritoryId(@Param("territoryId") String territoryId);

    @Query(
            value = """
                    SELECT COALESCE(MAX(serial_number), 0) FROM customers
                    WHERE territory_id = :territoryId
                    """,
            nativeQuery = true)
    int findMaxSerialNumberNative(@Param("territoryId") String territoryId);

    @Query(
            value = """
                    SELECT COALESCE(MAX(serial_number), 0) + 1
                    FROM customers
                    WHERE territory_id = :territoryId
                    """,
            nativeQuery = true)
    int allocateNextSerialNumber(@Param("territoryId") String territoryId);

    List<Customer> findByFullNameContainingIgnoreCase(String name);

    List<Customer> findByFullNameContainingIgnoreCaseAndBlockNameContainingIgnoreCase(String name, String block);
}
