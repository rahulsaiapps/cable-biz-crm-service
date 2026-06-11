package com.cablepulse.service;

import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.repository.EmployeeRepository;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import(EmployeeReconciliationService.class)
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
    void resolveEmployee_leavesNonPendingEmailMatchUntouched() {
        Employee owner = new Employee("owner-uid-1", "Owner User", EmployeeRole.OWNER);
        owner.setEmail("owner@example.com");
        employeeRepository.save(owner);

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("different-firebase-uid");
        when(token.getEmail()).thenReturn("owner@example.com");

        Employee resolved = reconciliationService.resolveEmployee(token);

        assertThat(resolved).isNull();
        assertThat(employeeRepository.findById("owner-uid-1")).isPresent();
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
}
