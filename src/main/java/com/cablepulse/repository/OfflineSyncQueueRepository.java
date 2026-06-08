package com.cablepulse.repository;

import com.cablepulse.model.OfflineSyncQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfflineSyncQueueRepository extends JpaRepository<OfflineSyncQueue, String> {

    Optional<OfflineSyncQueue> findByIdempotencyToken(String token);
}
