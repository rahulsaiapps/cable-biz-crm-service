package com.cablepulse.repository;

import com.cablepulse.model.GlobalPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlobalPlanRepository extends JpaRepository<GlobalPlan, String> {

    List<GlobalPlan> findByWorkspaceId(String workspaceId);

    @Query("SELECT p FROM GlobalPlan p LEFT JOIN FETCH p.provider WHERE p.workspaceId = :workspaceId")
    List<GlobalPlan> findAllWithProviderByWorkspaceId(@Param("workspaceId") String workspaceId);

    @Query("""
            SELECT p FROM GlobalPlan p JOIN FETCH p.provider
            WHERE p.workspaceId = :workspaceId AND p.provider.name = :providerName
            """)
    List<GlobalPlan> findByProvider_NameWithProvider(
            @Param("workspaceId") String workspaceId,
            @Param("providerName") String providerName);

    Optional<GlobalPlan> findByPlanIdAndWorkspaceId(String planId, String workspaceId);
}
