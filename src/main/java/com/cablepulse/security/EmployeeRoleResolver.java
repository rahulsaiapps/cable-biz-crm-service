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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for mapping Firebase tokens to Spring Security roles.
 * Unknown or unprovisioned users receive {@link EmployeeRole#COLLECTION_BOY} — never OWNER.
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

    public List<GrantedAuthority> resolveAuthorities(FirebaseToken decodedToken) {
        Map<String, Object> claims = decodedToken.getClaims();
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (claims.containsKey("role")) {
            mapClaimRole(String.valueOf(claims.get("role")), authorities);
        }
        if (Boolean.TRUE.equals(claims.get("owner"))) {
            addRoleIfMissing(authorities, EmployeeRole.OWNER);
        }
        if (Boolean.TRUE.equals(claims.get("collection_boy"))) {
            addRoleIfMissing(authorities, EmployeeRole.COLLECTION_BOY);
        }

        if (authorities.isEmpty()) {
            EmployeeRole role = resolveEmployeeRole(decodedToken);
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        }

        return authorities;
    }

    public String resolveRoleClaim(FirebaseToken decodedToken) {
        List<GrantedAuthority> authorities = resolveAuthorities(decodedToken);
        for (GrantedAuthority authority : authorities) {
            if ("ROLE_OWNER".equals(authority.getAuthority())) {
                return "ROLE_OWNER";
            }
        }
        if (!authorities.isEmpty()) {
            return authorities.get(0).getAuthority();
        }
        return "ROLE_COLLECTION_BOY";
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

    private static void mapClaimRole(String rawRole, List<GrantedAuthority> authorities) {
        String role = rawRole.toUpperCase();
        if (role.equals("OWNER") || role.equals("ROLE_OWNER")) {
            addRoleIfMissing(authorities, EmployeeRole.OWNER);
        } else if (role.equals("COLLECTION_BOY") || role.equals("ROLE_COLLECTION_BOY")) {
            addRoleIfMissing(authorities, EmployeeRole.COLLECTION_BOY);
        }
    }

    private static void addRoleIfMissing(List<GrantedAuthority> authorities, EmployeeRole role) {
        String authority = "ROLE_" + role.name();
        if (authorities.stream().noneMatch(a -> a.getAuthority().equals(authority))) {
            authorities.add(new SimpleGrantedAuthority(authority));
        }
    }
}
