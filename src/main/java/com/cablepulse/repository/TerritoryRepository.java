package com.cablepulse.repository;

import com.cablepulse.model.Territory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TerritoryRepository extends JpaRepository<Territory, String> {

    @Query("SELECT DISTINCT t.locationName FROM Territory t WHERE t.deleted = false ORDER BY t.locationName")
    List<String> findDistinctActiveLocationNames();
}
