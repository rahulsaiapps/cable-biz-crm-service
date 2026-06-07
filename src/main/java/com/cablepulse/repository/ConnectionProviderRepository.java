package com.cablepulse.repository;

import com.cablepulse.model.ConnectionProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConnectionProviderRepository extends JpaRepository<ConnectionProvider, Long> {
}
