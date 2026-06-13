package com.cablepulse.repository;

import com.cablepulse.model.DailyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DailyTransactionRepository extends JpaRepository<DailyTransaction, String> {

    List<DailyTransaction> findByRecordedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT t FROM DailyTransaction t
            WHERE t.recordedAt >= :start AND t.recordedAt <= :end
              AND t.customer.workspaceId = :workspaceId
            """)
    List<DailyTransaction> findByWorkspaceIdAndRecordedAtBetween(
            @Param("workspaceId") String workspaceId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    List<DailyTransaction> findByFieldAgent_EmployeeIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            String employeeId, LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(t.amountCollected), 0)
            FROM DailyTransaction t
            WHERE t.fieldAgent.employeeId = :employeeId
              AND t.recordedAt >= :start
              AND t.recordedAt <= :end
            """)
    BigDecimal sumAmountCollectedByAgentBetween(
            @Param("employeeId") String employeeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(t.amountCollected), 0)
            FROM DailyTransaction t
            WHERE t.recordedAt >= :start AND t.recordedAt <= :end
            """)
    BigDecimal sumAmountCollectedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(t.amountCollected), 0)
            FROM DailyTransaction t
            WHERE t.recordedAt >= :start AND t.recordedAt <= :end
              AND t.customer.workspaceId = :workspaceId
            """)
    BigDecimal sumAmountCollectedBetweenForWorkspace(
            @Param("workspaceId") String workspaceId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Total customer collections for a calendar month within a workspace.
     */
    default BigDecimal sumAmountCollectedForCalendarMonth(String workspaceId, java.time.YearMonth month) {
        java.time.LocalDateTime start = month.atDay(1).atStartOfDay();
        java.time.LocalDateTime end = month.atEndOfMonth().atTime(java.time.LocalTime.MAX);
        BigDecimal total = sumAmountCollectedBetweenForWorkspace(workspaceId, start, end);
        return total != null ? total : BigDecimal.ZERO;
    }
}
