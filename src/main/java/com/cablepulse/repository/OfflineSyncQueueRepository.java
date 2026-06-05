package com.cablepulse.repository;

import com.cablepulse.model.OfflineSyncQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfflineSyncQueueRepository extends JpaRepository<OfflineSyncQueue, Long> {

    Optional<OfflineSyncQueue> findByIdempotencyToken(UUID token);
}
