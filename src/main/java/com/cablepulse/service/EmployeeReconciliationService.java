package com.cablepulse.service;

import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.repository.EmployeeRepository;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EmployeeReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeReconciliationService.class);

    static final String PENDING_ID_PREFIX = "PENDING-";

    private final EmployeeRepository employeeRepository;
    private final Set<String> bootstrapOwnerEmails;

    public EmployeeReconciliationService(
            EmployeeRepository employeeRepository,
            @Value("${cablepulse.security.bootstrap-owner-emails:}") String bootstrapOwnerEmails) {
        this.employeeRepository = employeeRepository;
        this.bootstrapOwnerEmails = parseEmailAllowlist(bootstrapOwnerEmails);
    }

    /**
     * Resolves the authenticated user's Employee row, claiming a pre-provisioned
     * PENDING-* placeholder on first sign-in when the Firebase UID is not yet linked.
     */
    public Employee resolveEmployee(FirebaseToken decodedToken) {
        try {
            String firebaseUid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            return employeeRepository.findById(firebaseUid)
                    .or(() -> reconcilePendingEmployee(firebaseUid, email))
                    .or(() -> relinkEmployeeByEmail(firebaseUid, email))
                    .or(() -> bootstrapOwnerIfMissing(decodedToken))
                    .or(() -> registerOwnerOnGoogleSignIn(decodedToken))
                    .orElse(null);
        } catch (Exception ex) {
            logger.warn(
                    "resolveEmployee degraded for uid={}: {}",
                    decodedToken.getUid(),
                    ex.getMessage());
            return null;
        }
    }

    public Optional<Employee> findEmployeeById(String firebaseUid) {
        return employeeRepository.findById(firebaseUid);
    }

    /**
     * When the workspace has no OWNER yet, the first Firebase sign-in is
     * promoted to OWNER so the operator can manage customers and territories.
     */
    @Transactional
    Optional<Employee> bootstrapOwnerIfMissing(FirebaseToken decodedToken) {
        boolean ownerExists = employeeRepository.findAll().stream()
                .anyMatch(employee -> employee.getRole() == EmployeeRole.OWNER);
        if (ownerExists) {
            return Optional.empty();
        }

        String email = decodedToken.getEmail();
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        if (!isBootstrapOwnerEmailAllowed(email)) {
            logger.warn(
                    "Skipping owner bootstrap for uid={}: email not in bootstrap allowlist",
                    decodedToken.getUid());
            return Optional.empty();
        }

        String firebaseUid = decodedToken.getUid();
        String name = decodedToken.getName();
        String fullName = (name != null && !name.isBlank()) ? name.trim() : "Workspace Owner";

        Employee owner = new Employee(
                firebaseUid,
                fullName,
                EmployeeRole.OWNER);
        if (decodedToken.getEmail() != null && !decodedToken.getEmail().isBlank()) {
            owner.setEmail(decodedToken.getEmail().trim());
        }

        return Optional.of(employeeRepository.save(owner));
    }

    /**
     * Links a manually provisioned row (same email, different Firebase UID) to the
     * UID from the current Google sign-in.
     */
    @Transactional
    Optional<Employee> relinkEmployeeByEmail(String firebaseUid, String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return employeeRepository.findByEmailIgnoreCase(email.trim())
                .filter(existing -> !firebaseUid.equals(existing.getEmployeeId()))
                .filter(existing -> !isPendingPlaceholder(existing))
                .map(existing -> {
                    Employee linked = new Employee(
                            firebaseUid,
                            existing.getFullName(),
                            existing.getRole() != null ? existing.getRole() : EmployeeRole.OWNER);
                    linked.setEmail(existing.getEmail());
                    linked.setDescription(existing.getDescription());
                    if (existing.getAssignedVillages() != null) {
                        linked.setAssignedVillages(new java.util.ArrayList<>(existing.getAssignedVillages()));
                    }

                    employeeRepository.delete(existing);
                    employeeRepository.flush();
                    logger.info(
                            "Relinked employee email={} from uid={} to uid={}",
                            email,
                            existing.getEmployeeId(),
                            firebaseUid);
                    return employeeRepository.save(linked);
                });
    }

    /**
     * First Google sign-in for an unknown email — register as workspace owner so
     * operators can use the app without manual Supabase inserts.
     */
    @Transactional
    Optional<Employee> registerOwnerOnGoogleSignIn(FirebaseToken decodedToken) {
        String email = decodedToken.getEmail();
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        if (employeeRepository.findByEmailIgnoreCase(email.trim()).isPresent()) {
            return Optional.empty();
        }

        String firebaseUid = decodedToken.getUid();
        String name = decodedToken.getName();
        String fullName = (name != null && !name.isBlank()) ? name.trim() : "Workspace Owner";

        Employee owner = new Employee(firebaseUid, fullName, EmployeeRole.OWNER);
        owner.setEmail(email.trim());
        logger.info("Registered Google sign-in as OWNER uid={} email={}", firebaseUid, email);
        return Optional.of(employeeRepository.save(owner));
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

    private boolean isBootstrapOwnerEmailAllowed(String email) {
        if (bootstrapOwnerEmails.isEmpty()) {
            return false;
        }
        return bootstrapOwnerEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }

    private static Set<String> parseEmailAllowlist(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
