package com.cablepulse.service;

import com.cablepulse.model.AuditEvent;
import com.cablepulse.repository.AuditEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogWriter auditLogWriter;

    @InjectMocks
    private AuditLogService auditLogService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logCapturesActorUidFromSecurityContextAndDelegatesToWriter() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "firebase-uid-42",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));

        auditLogService.log(
                AuditLogService.RECORD_PAYMENT,
                "cust_001",
                "{\"amount\":350}");

        verify(auditLogWriter).write(
                "firebase-uid-42",
                AuditLogService.RECORD_PAYMENT,
                "cust_001",
                "{\"amount\":350}");
    }
}

@ExtendWith(MockitoExtension.class)
class AuditLogWriterTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditLogWriter auditLogWriter;

    @Test
    void writePersistsAuditEvent() {
        auditLogWriter.write(
                "firebase-uid-42",
                AuditLogService.CREATE_EXPENSE,
                "99",
                "{\"amount\":500}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getActorUid()).isEqualTo("firebase-uid-42");
        assertThat(saved.getActionType()).isEqualTo(AuditLogService.CREATE_EXPENSE);
        assertThat(saved.getEntityId()).isEqualTo("99");
        assertThat(saved.getDetails()).contains("500");
        assertThat(saved.getRecordedAt()).isNotNull();
    }
}
