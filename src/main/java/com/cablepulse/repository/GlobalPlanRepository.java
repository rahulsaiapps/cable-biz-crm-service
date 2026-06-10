package com.cablepulse.repository;

import com.cablepulse.model.GlobalPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlobalPlanRepository extends JpaRepository<GlobalPlan, String> {
    List<GlobalPlan> findByProvider_Name(String providerName);

    @Query("SELECT p FROM GlobalPlan p LEFT JOIN FETCH p.provider")
    List<GlobalPlan> findAllWithProvider();

    @Query("SELECT p FROM GlobalPlan p JOIN FETCH p.provider WHERE p.provider.name = :providerName")
    List<GlobalPlan> findByProvider_NameWithProvider(String providerName);
}
