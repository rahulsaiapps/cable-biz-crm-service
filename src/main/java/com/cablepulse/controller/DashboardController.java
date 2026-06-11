package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.security.SecurityAuth;
import com.cablepulse.service.DashboardService;
import com.cablepulse.util.EtagSupport;
import org.springframework.http.ResponseEntity;
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
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        DashboardData dashboardData = dashboardService.getDashboardMetrics(SecurityAuth.isOwner());

        return EtagSupport.respondWithEtag(ifNoneMatch, dashboardData, () -> {
            StandardResponse_DashboardData response = new StandardResponse_DashboardData(
                    LocalDateTime.now(),
                    "SUCCESS",
                    null,
                    dashboardData
            );
            return ResponseEntity.ok(response);
        });
    }
}
