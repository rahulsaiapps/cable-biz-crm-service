package com.cablepulse.repository;

import com.cablepulse.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    List<Customer> findByTerritory_TerritoryId(String territoryId);

    List<Customer> findByTerritory_TerritoryIdAndBlockName(String territoryId, String blockName);

    List<Customer> findByFullNameContainingIgnoreCase(String name);

    List<Customer> findByFullNameContainingIgnoreCaseAndBlockNameContainingIgnoreCase(String name, String block);
}
