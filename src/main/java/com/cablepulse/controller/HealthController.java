package com.cablepulse.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class HealthController {

    private final String latestStableVersion;
    private final String minimumRequiredVersion;
    private final boolean isCriticalPatch;

    public HealthController(
            @Value("${cablepulse.app.latest-stable-version:1.0.10}") String latestStableVersion,
            @Value("${cablepulse.app.minimum-required-version:1.0.1}") String minimumRequiredVersion,
            @Value("${cablepulse.app.is-critical-patch:false}") boolean isCriticalPatch) {
        this.latestStableVersion = latestStableVersion;
        this.minimumRequiredVersion = minimumRequiredVersion;
        this.isCriticalPatch = isCriticalPatch;
    }

    @GetMapping("/health")
    public Map<String, Object> checkHealth() {
        return Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "platform", "Cable Pulse Utility Backend");
    }

    /**
     * Public version check for the mobile app. No login required.
     * Workers compare their installed version against these values.
     */
    @GetMapping("/app-version-check")
    public Map<String, Object> checkAppVersion() {
        return Map.of(
                "latest_stable_version", latestStableVersion,
                "minimum_required_version", minimumRequiredVersion,
                "is_critical_patch", isCriticalPatch);
    }
}
