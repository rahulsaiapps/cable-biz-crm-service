package com.cablepulse.repository;

import com.cablepulse.model.GlobalPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalPlanRepository extends JpaRepository<GlobalPlan, String> {
}
