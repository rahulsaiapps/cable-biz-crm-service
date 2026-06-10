package com.cablepulse.repository;

import com.cablepulse.model.ConnectionProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConnectionProviderRepository extends JpaRepository<ConnectionProvider, Long> {

    Optional<ConnectionProvider> findByName(String name);
}
