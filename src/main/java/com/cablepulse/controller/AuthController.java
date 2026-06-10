package com.cablepulse.controller;

import com.cablepulse.model.Employee;
import com.cablepulse.service.EmployeeReconciliationService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final FirebaseAuth firebaseAuth;
    private final EmployeeReconciliationService employeeReconciliationService;

    public AuthController(
            FirebaseAuth firebaseAuth,
            EmployeeReconciliationService employeeReconciliationService) {
        this.firebaseAuth = firebaseAuth;
        this.employeeReconciliationService = employeeReconciliationService;
    }

    @PostMapping("/token-swap")
    public ResponseEntity<Map<String, Object>> tokenSwap(@RequestBody TokenSwapRequest request) {
        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(request.firebaseIdToken());
            String uid = decodedToken.getUid();
            String name = decodedToken.getName();
            
            Employee employee = employeeReconciliationService.resolveEmployee(decodedToken);
            String fullName = employee != null ? employee.getFullName() : (name != null ? name : "Rahul Sai");
            String role = employee != null ? "ROLE_" + employee.getRole().name() : "ROLE_OWNER";

            Map<String, Object> userProfile = new LinkedHashMap<>();
            userProfile.put("userId", uid);
            userProfile.put("fullName", fullName);
            userProfile.put("role", role);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("accessToken", request.firebaseIdToken());
            data.put("accessTokenExpiresInSeconds", 3600);
            data.put("refreshToken", "mock-refresh-token");
            data.put("userProfile", userProfile);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("status", "SUCCESS");
            response.put("error", null);
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("status", "ERROR");
            response.put("error", "Token validation failed: " + e.getMessage());
            response.put("data", null);
            return ResponseEntity.status(400).body(response);
        }
    }

    public record TokenSwapRequest(String firebaseIdToken) {}
}
