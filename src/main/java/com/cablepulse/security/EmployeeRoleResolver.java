package com.cablepulse.security;

import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.service.EmployeeReconciliationService;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps authenticated users to Spring Security roles. API authorization always
 * uses the persisted {@code employees} row — never Firebase custom claims.
 */
@Component
public class EmployeeRoleResolver {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeRoleResolver.class);

    private final EmployeeReconciliationService employeeReconciliationService;
    private final EmployeeRepository employeeRepository;

    public EmployeeRoleResolver(
            EmployeeReconciliationService employeeReconciliationService,
            EmployeeRepository employeeRepository) {
        this.employeeReconciliationService = employeeReconciliationService;
        this.employeeRepository = employeeRepository;
    }

    /** Used only during token-swap when provisioning/reconciling the employee row. */
    public List<GrantedAuthority> resolveAuthorities(FirebaseToken decodedToken) {
        EmployeeRole role = resolveEmployeeRole(decodedToken);
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public String resolveRoleClaim(FirebaseToken decodedToken) {
        EmployeeRole role = resolveEmployeeRole(decodedToken);
        return "ROLE_" + role.name();
    }

    /** Resolves the role claim for a persisted Firebase UID (refresh-token flow). */
    public String resolveRoleForUserId(String userId) {
        return employeeRepository.findById(userId)
                .map(employee -> employee.getRole() != null
                        ? "ROLE_" + employee.getRole().name()
                        : "ROLE_COLLECTION_BOY")
                .orElse("ROLE_COLLECTION_BOY");
    }

    public List<GrantedAuthority> resolveAuthoritiesForUserId(String userId, String roleClaim) {
        return List.of(new SimpleGrantedAuthority(roleClaim));
    }

    private EmployeeRole resolveEmployeeRole(FirebaseToken decodedToken) {
        try {
            Employee employee = employeeReconciliationService.resolveEmployee(decodedToken);
            if (employee == null) {
                logger.warn(
                        "No employee row for uid={}; assigning least-privilege COLLECTION_BOY",
                        decodedToken.getUid());
                return EmployeeRole.COLLECTION_BOY;
            }
            EmployeeRole role = employee.getRole();
            if (role == null) {
                logger.warn(
                        "Null role on employee uid={}; assigning COLLECTION_BOY",
                        decodedToken.getUid());
                return EmployeeRole.COLLECTION_BOY;
            }
            return role;
        } catch (Exception ex) {
            logger.warn(
                    "Employee reconciliation failed for uid={}; assigning COLLECTION_BOY: {}",
                    decodedToken.getUid(),
                    ex.getMessage());
            return EmployeeRole.COLLECTION_BOY;
        }
    }
}
