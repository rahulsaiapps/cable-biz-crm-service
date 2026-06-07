package com.cablepulse.repository;

import com.cablepulse.model.GlobalPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlobalPlanRepository extends JpaRepository<GlobalPlan, String> {
    List<GlobalPlan> findByProvider_Name(String providerName);
}
