package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<StandardResponse_DashboardData> getDashboardMetrics(
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        boolean isCollectionBoy = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COLLECTION_BOY"));

        DashboardData dashboardData = dashboardService.getDashboardMetrics(!isCollectionBoy);
        StandardResponse_DashboardData response = new StandardResponse_DashboardData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                dashboardData
        );

        return ResponseEntity.ok(response);
    }
}
