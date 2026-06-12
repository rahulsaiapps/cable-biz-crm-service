package com.cablepulse.repository;

import com.cablepulse.model.DailyExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DailyExpenseRepository extends JpaRepository<DailyExpense, Long> {

    List<DailyExpense> findByLoggedAtBetween(LocalDateTime start, LocalDateTime end);

    List<DailyExpense> findByLoggedByEmployeeIdAndLoggedAtBetweenOrderByLoggedAtDesc(
            String loggedByEmployeeId, LocalDateTime start, LocalDateTime end);
}
