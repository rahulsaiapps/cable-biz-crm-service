package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.service.FinanceAnalyticsService;
import com.cablepulse.service.NotificationDispatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('OWNER')")
public class NotificationController {

    private final NotificationDispatchService notificationDispatchService;
    private final FinanceAnalyticsService financeAnalyticsService;

    public NotificationController(
            NotificationDispatchService notificationDispatchService,
            FinanceAnalyticsService financeAnalyticsService) {
        this.notificationDispatchService = notificationDispatchService;
        this.financeAnalyticsService = financeAnalyticsService;
    }

    @PostMapping("/api/v1/broadcasts/pending-reminder")
    public ResponseEntity<StandardResponse_BroadcastAccepted> sendPendingReminder(
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        String trackingId = notificationDispatchService.sendPendingReminder();
        return acceptedBroadcast(trackingId);
    }

    @PostMapping("/api/v1/broadcasts/active-reminder")
    public ResponseEntity<StandardResponse_BroadcastAccepted> sendActiveReminder(
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        String trackingId = notificationDispatchService.sendActiveReminder();
        return acceptedBroadcast(trackingId);
    }

    @PostMapping("/api/v1/bulletins/outage")
    public ResponseEntity<StandardResponse_BroadcastAccepted> createOutageBulletin(
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        String title = stringValue(body.get("title"));
        String bulletinBody = stringValue(body.get("body"));
        String territoryId = stringValue(body.get("territory_id"));
        String trackingId = notificationDispatchService.createOutageBulletin(title, bulletinBody, territoryId);
        return acceptedBroadcast(trackingId);
    }

    @PostMapping("/api/v1/notifications/broadcast-outage")
    public ResponseEntity<StandardResponse_BroadcastAccepted> broadcastOutage(
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        String title = stringValue(body.get("title"));
        String alertBody = stringValue(body.get("body"));
        String territoryId = stringValue(body.get("territory_id"));
        String trackingId = notificationDispatchService.broadcastOutage(title, alertBody, territoryId);
        return acceptedBroadcast(trackingId);
    }

    @PostMapping("/api/v1/notifications/dispatched-alert")
    public ResponseEntity<StandardResponse_BroadcastAccepted> dispatchCustomerAlert(
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        String customerId = stringValue(body.get("customerId"));
        List<Integer> months = parseMonths(body.get("months"));
        String message = stringValue(body.get("message"));
        String trackingId = notificationDispatchService.dispatchCustomerAlert(customerId, months, message);
        return acceptedBroadcast(trackingId);
    }

    @PostMapping("/api/v1/notifications/dispatch-alert")
    public ResponseEntity<StandardResponse_NotificationDispatch> dispatchGenericAlert(
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        String trackingId = notificationDispatchService.dispatchGenericAlert(body);
        StandardResponse_NotificationDispatch response = new StandardResponse_NotificationDispatch(
                LocalDateTime.now(),
                "ACCEPTED",
                null,
                new NotificationDispatchData(trackingId, 1)
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/api/v1/alerts/target-size")
    public ResponseEntity<AlertTargetSizeDTO> getTargetAudienceSize(
            @RequestParam("region") String region,
            @RequestParam("block") String block,
            @RequestParam(value = "customer_types", required = false) String customerTypes) {

        List<String> types = customerTypes == null || customerTypes.isBlank()
                ? List.of()
                : Arrays.asList(customerTypes.split(","));
        int size = financeAnalyticsService.estimateAlertAudienceSize(region, block, types);
        return ResponseEntity.ok(new AlertTargetSizeDTO(size));
    }

    private ResponseEntity<StandardResponse_BroadcastAccepted> acceptedBroadcast(String trackingId) {
        StandardResponse_BroadcastAccepted response = new StandardResponse_BroadcastAccepted(
                LocalDateTime.now(),
                "ACCEPTED",
                null,
                new BroadcastAcceptedData(trackingId)
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    private static String stringValue(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> parseMonths(Object monthsObj) {
        if (monthsObj instanceof List<?> list) {
            return list.stream().map(item -> Integer.parseInt(String.valueOf(item))).toList();
        }
        return List.of();
    }
}
