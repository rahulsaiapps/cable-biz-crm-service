package com.cablepulse.service;

import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.model.Workspace;
import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.repository.WorkspaceRepository;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({EmployeeReconciliationService.class, WorkspaceService.class})
@TestPropertySource(properties = "cablepulse.security.bootstrap-owner-emails=rahul@example.com")
class EmployeeReconciliationServiceTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private EmployeeReconciliationService reconciliationService;

    @BeforeEach
    void seedLegacyWorkspace() {
        workspaceRepository.save(new Workspace(
                WorkspaceService.LEGACY_WORKSPACE_ID, "Legacy", null));
    }

    @Test
    void resolveEmployee_claimsPendingRowByEmailOnFirstSignIn() {
        Employee pending = new Employee("PENDING-abc-123", "Ramesh Kumar", EmployeeRole.COLLECTION_BOY);
        pending.setEmail("ramesh@example.com");
        pending.setWorkspaceId(WorkspaceService.LEGACY_WORKSPACE_ID);
        employeeRepository.save(pending);

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("firebase-uid-xyz");
        when(token.getEmail()).thenReturn("ramesh@example.com");

        Employee resolved = reconciliationService.resolveEmployee(token);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getEmployeeId()).isEqualTo("firebase-uid-xyz");
        assertThat(resolved.getWorkspaceId()).isEqualTo(WorkspaceService.LEGACY_WORKSPACE_ID);
        assertThat(employeeRepository.findById("PENDING-abc-123")).isEmpty();
    }

    @Test
    void resolveEmployee_relinksManualRowWhenEmailMatchesButUidDiffers() {
        Employee owner = new Employee("manual-uid-1", "Owner User", EmployeeRole.OWNER);
        owner.setEmail("owner@example.com");
        owner.setWorkspaceId(WorkspaceService.LEGACY_WORKSPACE_ID);
        employeeRepository.save(owner);

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("different-firebase-uid");
        when(token.getEmail()).thenReturn("owner@example.com");

        Employee resolved = reconciliationService.resolveEmployee(token);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getEmployeeId()).isEqualTo("different-firebase-uid");
        assertThat(employeeRepository.findById("manual-uid-1")).isEmpty();
    }

    @Test
    void resolveEmployee_registersOwnerForNewGoogleEmailWhenOwnerExists() {
        Employee existingOwner = new Employee("owner-uid-1", "Owner User", EmployeeRole.OWNER);
        existingOwner.setEmail("owner@example.com");
        existingOwner.setWorkspaceId(WorkspaceService.LEGACY_WORKSPACE_ID);
        employeeRepository.save(existingOwner);

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("new-operator-uid");
        when(token.getName()).thenReturn("New Operator");
        when(token.getEmail()).thenReturn("new-operator@example.com");

        Employee resolved = reconciliationService.resolveEmployee(token);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getEmployeeId()).isEqualTo("new-operator-uid");
        assertThat(resolved.getRole()).isEqualTo(EmployeeRole.OWNER);
        assertThat(resolved.getWorkspaceId()).isNotEqualTo(WorkspaceService.LEGACY_WORKSPACE_ID);
    }

    @Test
    void resolveEmployee_bootstrapsOwnerWhenNoOwnerExists() {
        Employee agent = new Employee("agent-uid", "Field Agent", EmployeeRole.COLLECTION_BOY);
        agent.setEmail("agent@example.com");
        agent.setWorkspaceId(WorkspaceService.LEGACY_WORKSPACE_ID);
        employeeRepository.save(agent);

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("operator-firebase-uid");
        when(token.getName()).thenReturn("Rahul Sai");
        when(token.getEmail()).thenReturn("rahul@example.com");

        Employee resolved = reconciliationService.resolveEmployee(token);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getEmployeeId()).isEqualTo("operator-firebase-uid");
        assertThat(resolved.getRole()).isEqualTo(EmployeeRole.OWNER);
        assertThat(resolved.getWorkspaceId()).isEqualTo(WorkspaceService.LEGACY_WORKSPACE_ID);
    }

    @Test
    void resolveEmployee_registersOwnerForUnknownEmailWhenBootstrapAllowlistMisses() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("operator-firebase-uid");
        when(token.getName()).thenReturn("Rahul Sai");
        when(token.getEmail()).thenReturn("not-allowed@example.com");

        Employee resolved = reconciliationService.resolveEmployee(token);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getEmployeeId()).isEqualTo("operator-firebase-uid");
        assertThat(resolved.getRole()).isEqualTo(EmployeeRole.OWNER);
        assertThat(resolved.getWorkspaceId()).startsWith("ws_");
    }
}
