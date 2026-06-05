package com.cablepulse.repository;

import com.cablepulse.model.DailyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DailyTransactionRepository extends JpaRepository<DailyTransaction, String> {

    List<DailyTransaction> findByRecordedAtBetween(LocalDateTime start, LocalDateTime end);
}
