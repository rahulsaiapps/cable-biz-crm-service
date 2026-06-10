package com.cablepulse.service;

import com.cablepulse.model.Employee;
import com.cablepulse.repository.EmployeeRepository;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EmployeeReconciliationService {

    static final String PENDING_ID_PREFIX = "PENDING-";

    private final EmployeeRepository employeeRepository;

    public EmployeeReconciliationService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Resolves the authenticated user's Employee row, claiming a pre-provisioned
     * PENDING-* placeholder on first sign-in when the Firebase UID is not yet linked.
     */
    public Employee resolveEmployee(FirebaseToken decodedToken) {
        String firebaseUid = decodedToken.getUid();

        return employeeRepository.findById(firebaseUid)
                .or(() -> reconcilePendingEmployee(firebaseUid, decodedToken.getEmail()))
                .orElse(null);
    }

    @Transactional
    public Optional<Employee> reconcilePendingEmployee(String firebaseUid, String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return employeeRepository.findByEmailIgnoreCase(email.trim())
                .filter(this::isPendingPlaceholder)
                .map(pending -> claimPendingEmployee(pending, firebaseUid));
    }

    private boolean isPendingPlaceholder(Employee employee) {
        String employeeId = employee.getEmployeeId();
        return employeeId != null && employeeId.startsWith(PENDING_ID_PREFIX);
    }

    private Employee claimPendingEmployee(Employee pending, String firebaseUid) {
        Employee claimed = new Employee(firebaseUid, pending.getFullName(), pending.getRole());
        claimed.setEmail(pending.getEmail());

        employeeRepository.delete(pending);
        employeeRepository.flush();

        return employeeRepository.save(claimed);
    }
}
