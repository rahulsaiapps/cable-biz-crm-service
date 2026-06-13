package com.cablepulse.repository;

import com.cablepulse.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    long countByWorkspaceId(String workspaceId);

    @Query("""
            SELECT COUNT(DISTINCT c.customerId) FROM Customer c
            WHERE c.workspaceId = :workspaceId
              AND ((c.globalPlan IS NULL AND c.customRateOverride IS NULL)
               OR EXISTS (
                   SELECT 1 FROM CustomerLedger l
                   WHERE l.customer.customerId = c.customerId
                     AND l.dueAmount > 0
               ))
            """)
    long countPendingCustomersByWorkspaceId(@Param("workspaceId") String workspaceId);

    default long countPendingCustomers() {
        throw new UnsupportedOperationException("Use countPendingCustomersByWorkspaceId");
    }

    @Query("""
            SELECT c FROM Customer c
            LEFT JOIN FETCH c.globalPlan
            WHERE c.territory.territoryId = :territoryId
              AND c.workspaceId = :workspaceId
            """)
    List<Customer> findByTerritoryIdWithPlan(
            @Param("workspaceId") String workspaceId,
            @Param("territoryId") String territoryId);

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
                      AND is_deleted = false
                    """,
            nativeQuery = true)
    int findMaxSerialNumberNative(@Param("territoryId") String territoryId);

    @Query(
            value = """
                    SELECT COALESCE(MAX(serial_number), 0) + 1
                    FROM customers
                    WHERE territory_id = :territoryId
                      AND is_deleted = false
                    """,
            nativeQuery = true)
    int allocateNextSerialNumber(@Param("territoryId") String territoryId);

    @Query(
            value = """
                    SELECT COUNT(*) FROM customers c
                    WHERE c.territory_id = :territoryId
                      AND c.is_deleted = false
                      AND COALESCE((
                          SELECT SUM(l.due_amount) FROM customer_ledgers l
                          WHERE l.customer_id = c.customer_id
                      ), 0) <= 0
                    """,
            nativeQuery = true)
    long countPaidCustomersByTerritoryId(@Param("territoryId") String territoryId);

    @Query(
            value = """
                    SELECT COUNT(*) FROM customers c
                    WHERE c.territory_id = :territoryId
                      AND c.is_deleted = false
                      AND COALESCE((
                          SELECT SUM(l.due_amount) FROM customer_ledgers l
                          WHERE l.customer_id = c.customer_id
                      ), 0) > 0
                    """,
            nativeQuery = true)
    long countPendingCustomersByTerritoryId(@Param("territoryId") String territoryId);

    List<Customer> findByWorkspaceId(String workspaceId);

    List<Customer> findByFullNameContainingIgnoreCaseAndWorkspaceId(String name, String workspaceId);

    List<Customer> findByFullNameContainingIgnoreCaseAndBlockNameContainingIgnoreCaseAndWorkspaceId(
            String name, String block, String workspaceId);

    Optional<Customer> findByCustomerIdAndWorkspaceId(String customerId, String workspaceId);
}
