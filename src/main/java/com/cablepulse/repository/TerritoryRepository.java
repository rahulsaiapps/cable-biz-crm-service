package com.cablepulse.repository;

import com.cablepulse.model.Territory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TerritoryRepository extends JpaRepository<Territory, String> {

    List<Territory> findByWorkspaceId(String workspaceId);

    @Query("""
            SELECT DISTINCT t.locationName FROM Territory t
            WHERE t.deleted = false AND t.workspaceId = :workspaceId
            ORDER BY t.locationName
            """)
    List<String> findDistinctActiveLocationNames(@Param("workspaceId") String workspaceId);

    Optional<Territory> findByWorkspaceIdAndLocationNameIgnoreCase(
            String workspaceId, String locationName);
}
