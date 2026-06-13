package com.cablepulse.controller;

import com.cablepulse.model.Employee;
import com.cablepulse.security.EmployeeRoleResolver;
import com.cablepulse.security.JwtTokenService;
import com.cablepulse.security.WorkspaceAuthorizationService;
import com.cablepulse.service.EmployeeReconciliationService;
import com.cablepulse.service.WorkspaceService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final FirebaseAuth firebaseAuth;
    private final EmployeeReconciliationService employeeReconciliationService;
    private final EmployeeRoleResolver employeeRoleResolver;
    private final JwtTokenService jwtTokenService;
    private final WorkspaceService workspaceService;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final boolean allowJwtFallback;

    public AuthController(
            FirebaseAuth firebaseAuth,
            EmployeeReconciliationService employeeReconciliationService,
            EmployeeRoleResolver employeeRoleResolver,
            JwtTokenService jwtTokenService,
            WorkspaceService workspaceService,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            @Value("${cablepulse.security.allow-jwt-fallback:false}") boolean allowJwtFallback) {
        this.firebaseAuth = firebaseAuth;
        this.employeeReconciliationService = employeeReconciliationService;
        this.employeeRoleResolver = employeeRoleResolver;
        this.jwtTokenService = jwtTokenService;
        this.workspaceService = workspaceService;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.allowJwtFallback = allowJwtFallback;
    }

    @PostMapping("/token-swap")
    public ResponseEntity<Map<String, Object>> tokenSwap(@RequestBody TokenSwapRequest request) {
        if (request.firebaseIdToken() == null || request.firebaseIdToken().isBlank()) {
            return ResponseEntity.status(400).body(errorResponse("firebaseIdToken is required"));
        }

        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(request.firebaseIdToken());
            String uid = decodedToken.getUid();
            String fullName = (decodedToken.getName() != null && !decodedToken.getName().isBlank())
                    ? decodedToken.getName().trim()
                    : "Operator";
            String role = "ROLE_COLLECTION_BOY";
            String businessName = null;

            Employee employee = null;
            try {
                employee = employeeReconciliationService.resolveEmployee(decodedToken);
                if (employee == null) {
                    return ResponseEntity.status(403).body(
                            errorResponse("No workspace account found for this Google sign-in. Ask your operator to invite you."));
                }
                if (employee.getFullName() != null && !employee.getFullName().isBlank()) {
                    fullName = employee.getFullName();
                }
                String workspaceId = employee.getWorkspaceId();
                if (workspaceId != null && !workspaceId.isBlank()) {
                    businessName = workspaceService.businessNameFor(workspaceId);
                }
                role = employeeRoleResolver.resolveRoleForUserId(uid);
            } catch (Exception reconciliationError) {
                logger.warn("Token-swap reconciliation failed for uid={}", uid);
                return ResponseEntity.status(503).body(
                        errorResponse("Account setup is temporarily unavailable. Please try again."));
            }

            return ResponseEntity.ok(
                    buildTokenResponse(uid, fullName, role, employee, businessName, request.firebaseIdToken()));
        } catch (Exception e) {
            logger.error("Token-swap failed", e);
            return ResponseEntity.status(401).body(errorResponse("Invalid or expired Firebase token"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody RefreshRequest request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            return ResponseEntity.status(400).body(errorResponse("refreshToken is required"));
        }

        try {
            Claims claims = jwtTokenService.parseClaims(request.refreshToken());
            if (!jwtTokenService.isRefreshToken(claims)) {
                return ResponseEntity.status(401).body(errorResponse("Invalid refresh token"));
            }

            String uid = claims.getSubject();
            Employee employee = employeeReconciliationService.findEmployeeById(uid).orElse(null);
            if (employee == null) {
                return ResponseEntity.status(401).body(
                        errorResponse("Account no longer exists. Please sign in again."));
            }
            String role = employeeRoleResolver.resolveRoleForUserId(uid);
            String fullName = employee.getFullName() != null && !employee.getFullName().isBlank()
                    ? employee.getFullName()
                    : "Operator";
            String workspaceId = employee.getWorkspaceId();
            String businessName = workspaceId != null
                    ? workspaceService.businessNameFor(workspaceId)
                    : null;

            return ResponseEntity.ok(buildTokenResponse(uid, fullName, role, employee, businessName, null));
        } catch (JwtException e) {
            return ResponseEntity.status(401).body(errorResponse("Refresh token expired or invalid"));
        }
    }

    private Map<String, Object> buildTokenResponse(
            String uid,
            String fullName,
            String role,
            Employee employee,
            String businessName,
            String firebaseFallbackToken) {
        String workspaceId = employee != null ? employee.getWorkspaceId() : null;
        String accessToken;
        String refreshToken;
        boolean usingFirebaseFallback = false;

        try {
            accessToken = jwtTokenService.createAccessToken(uid, role, workspaceId);
            refreshToken = jwtTokenService.createRefreshToken(uid);
        } catch (Exception jwtError) {
            if (!allowJwtFallback || firebaseFallbackToken == null || firebaseFallbackToken.isBlank()) {
                logger.error("JWT issuance failed for uid={}", uid, jwtError);
                throw new IllegalStateException("Unable to issue session tokens");
            }
            logger.warn("JWT issuance failed for uid={}; using dev Firebase token fallback", uid);
            usingFirebaseFallback = true;
            accessToken = firebaseFallbackToken;
            refreshToken = null;
        }

        Map<String, Object> userProfile = new LinkedHashMap<>();
        userProfile.put("userId", uid);
        userProfile.put("fullName", fullName);
        userProfile.put("role", role);
        if (workspaceId != null) {
            userProfile.put("workspaceId", workspaceId);
        }
        if (businessName != null) {
            userProfile.put("businessName", businessName);
        }
        if (employee != null && employee.getAssignedVillages() != null) {
            userProfile.put("assignedVillages", employee.getAssignedVillages());
        }
        if (employee != null) {
            List<String> territoryIds = workspaceAuthorizationService.resolveTerritoryIdsForEmployee(employee);
            if (!territoryIds.isEmpty()) {
                userProfile.put("assignedTerritoryIds", territoryIds);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accessToken", accessToken);
        data.put(
                "accessTokenExpiresInSeconds",
                usingFirebaseFallback ? 3600 : JwtTokenService.ACCESS_TTL_SECONDS);
        if (refreshToken != null) {
            data.put("refreshToken", refreshToken);
        }
        data.put("userProfile", userProfile);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "SUCCESS");
        response.put("error", null);
        response.put("data", data);
        return response;
    }

    private static Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "ERROR");
        response.put("error", message);
        response.put("data", null);
        return response;
    }

    public record TokenSwapRequest(String firebaseIdToken) {}

    public record RefreshRequest(String refreshToken) {}
}
