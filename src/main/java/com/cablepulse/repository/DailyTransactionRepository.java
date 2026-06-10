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
            SELECT COALESCE(SUM(t.amountCollected), 0)
            FROM DailyTransaction t
            WHERE t.recordedAt >= :start AND t.recordedAt <= :end
            """)
    BigDecimal sumAmountCollectedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
