package com.cablepulse.service;

import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.repository.EmployeeRepository;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import(EmployeeReconciliationService.class)
@TestPropertySource(properties = "cablepulse.security.bootstrap-owner-emails=rahul@example.com")
class EmployeeReconciliationServiceTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeReconciliationService reconciliationService;

    @Test
    void resolveEmployee_claimsPendingRowByEmailOnFirstSignIn() {
        Employee pending = new Employee("PENDING-abc-123", "Ramesh Kumar", EmployeeRole.COLLECTION_BOY);
        pending.setEmail("ramesh@example.com");
        employeeRepository.save(pending);

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("firebase-uid-xyz");
        when(token.getEmail()).thenReturn("ramesh@example.com");

        Employee resolved = reconciliationService.resolveEmployee(token);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getEmployeeId()).isEqualTo("firebase-uid-xyz");
        assertThat(resolved.getFullName()).isEqualTo("Ramesh Kumar");
        assertThat(resolved.getRole()).isEqualTo(EmployeeRole.COLLECTION_BOY);
        assertThat(resolved.getEmail()).isEqualTo("ramesh@example.com");
        assertThat(employeeRepository.findById("PENDING-abc-123")).isEmpty();
        assertThat(employeeRepository.findById("firebase-uid-xyz")).isPresent();
    }

    @Test
    void resolveEmployee_relinksManualRowWhenEmailMatchesButUidDiffers() {
        Employee owner = new Employee("manual-uid-1", "Owner User", EmployeeRole.OWNER);
        owner.setEmail("owner@example.com");
        employeeRepository.save(owner);

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("different-firebase-uid");
        when(token.getEmail()).thenReturn("owner@example.com");

        Employee resolved = reconciliationService.resolveEmployee(token);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getEmployeeId()).isEqualTo("different-firebase-uid");
        assertThat(resolved.getRole()).isEqualTo(EmployeeRole.OWNER);
        assertThat(employeeRepository.findById("manual-uid-1")).isEmpty();
        assertThat(employeeRepository.findById("different-firebase-uid")).isPresent();
    }

    @Test
    void resolveEmployee_registersOwnerForNewGoogleEmailWhenOwnerExists() {
        Employee existingOwner = new Employee("owner-uid-1", "Owner User", EmployeeRole.OWNER);
        existingOwner.setEmail("owner@example.com");
        employeeRepository.save(existingOwner);

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("new-operator-uid");
        when(token.getName()).thenReturn("New Operator");
        when(token.getEmail()).thenReturn("new-operator@example.com");

        Employee resolved = reconciliationService.resolveEmployee(token);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getEmployeeId()).isEqualTo("new-operator-uid");
        assertThat(resolved.getRole()).isEqualTo(EmployeeRole.OWNER);
        assertThat(resolved.getEmail()).isEqualTo("new-operator@example.com");
    }

    @Test
    void resolveEmployee_bootstrapsOwnerWhenNoOwnerExists() {
        Employee agent = new Employee("agent-uid", "Field Agent", EmployeeRole.COLLECTION_BOY);
        agent.setEmail("agent@example.com");
        employeeRepository.save(agent);

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("operator-firebase-uid");
        when(token.getName()).thenReturn("Rahul Sai");
        when(token.getEmail()).thenReturn("rahul@example.com");

        Employee resolved = reconciliationService.resolveEmployee(token);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getEmployeeId()).isEqualTo("operator-firebase-uid");
        assertThat(resolved.getRole()).isEqualTo(EmployeeRole.OWNER);
        assertThat(resolved.getEmail()).isEqualTo("rahul@example.com");
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
        assertThat(resolved.getEmail()).isEqualTo("not-allowed@example.com");
    }
}
