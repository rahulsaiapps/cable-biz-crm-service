package com.cablepulse.repository;

import com.cablepulse.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByActorUidOrderByRecordedAtDesc(String actorUid);

    List<AuditEvent> findByActionTypeOrderByRecordedAtDesc(String actionType);

    List<AuditEvent> findByEntityIdOrderByRecordedAtDesc(String entityId);

    List<AuditEvent> findByRecordedAtBetweenOrderByRecordedAtDesc(
            LocalDateTime start,
            LocalDateTime end);
}
