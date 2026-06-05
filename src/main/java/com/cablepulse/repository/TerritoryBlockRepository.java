package com.cablepulse.repository;

import com.cablepulse.model.TerritoryBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TerritoryBlockRepository extends JpaRepository<TerritoryBlock, Long> {
}
