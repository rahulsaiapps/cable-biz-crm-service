package com.cablepulse.controller;

import com.cablepulse.model.Employee;
import com.cablepulse.security.EmployeeRoleResolver;
import com.cablepulse.security.JwtTokenService;
import com.cablepulse.service.EmployeeReconciliationService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final FirebaseAuth firebaseAuth;
    private final EmployeeReconciliationService employeeReconciliationService;
    private final EmployeeRoleResolver employeeRoleResolver;
    private final JwtTokenService jwtTokenService;
    private final boolean allowJwtFallback;

    public AuthController(
            FirebaseAuth firebaseAuth,
            EmployeeReconciliationService employeeReconciliationService,
            EmployeeRoleResolver employeeRoleResolver,
            JwtTokenService jwtTokenService,
            @Value("${cablepulse.security.allow-jwt-fallback:false}") boolean allowJwtFallback) {
        this.firebaseAuth = firebaseAuth;
        this.employeeReconciliationService = employeeReconciliationService;
        this.employeeRoleResolver = employeeRoleResolver;
        this.jwtTokenService = jwtTokenService;
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
            String name = decodedToken.getName();

            String fullName = (name != null && !name.isBlank()) ? name.trim() : "Operator";
            String role = "ROLE_COLLECTION_BOY";

            try {
                Employee employee = employeeReconciliationService.resolveEmployee(decodedToken);
                if (employee != null && employee.getFullName() != null && !employee.getFullName().isBlank()) {
                    fullName = employee.getFullName();
                }
                role = employeeRoleResolver.resolveRoleClaim(decodedToken);
            } catch (Exception reconciliationError) {
                logger.warn("Token-swap reconciliation degraded for uid={}", uid);
            }

            return ResponseEntity.ok(buildTokenResponse(uid, fullName, role, request.firebaseIdToken()));
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
            String role = employeeRoleResolver.resolveRoleForUserId(uid);
            String fullName = employeeReconciliationService.findEmployeeById(uid)
                    .map(Employee::getFullName)
                    .orElse("Operator");

            return ResponseEntity.ok(buildTokenResponse(uid, fullName, role, null));
        } catch (JwtException e) {
            return ResponseEntity.status(401).body(errorResponse("Refresh token expired or invalid"));
        }
    }

    private Map<String, Object> buildTokenResponse(
            String uid, String fullName, String role, String firebaseFallbackToken) {
        String accessToken;
        String refreshToken;
        boolean usingFirebaseFallback = false;

        try {
            accessToken = jwtTokenService.createAccessToken(uid, role);
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
