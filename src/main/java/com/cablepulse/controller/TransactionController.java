package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.DailyExpense;
import com.cablepulse.model.IspSettlement;
import com.cablepulse.service.AuditLogService;
import com.cablepulse.service.DailyLedgerService;
import com.cablepulse.service.SyncService;
import com.cablepulse.util.EtagSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class TransactionController {

    private final SyncService syncService;
    private final DailyLedgerService dailyLedgerService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public TransactionController(
            SyncService syncService,
            DailyLedgerService dailyLedgerService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper) {
        this.syncService = syncService;
        this.dailyLedgerService = dailyLedgerService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/v1/sync/synchronize")
    public ResponseEntity<StandardResponse_SyncResolution> synchronizeOfflineQueue(
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId,
            @RequestBody SyncRequestPayload payload) {

        StandardResponse_SyncResolution response = syncService.processSyncBatch(payload);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/transactions/expense")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StandardResponse_ExpenseCreated> logExpense(@RequestBody DailyExpense expense) {
        StandardResponse_ExpenseCreated response = dailyLedgerService.saveExpense(expense);
        if (response.data() != null && response.data().expenseId() != null) {
            auditLogService.log(
                    AuditLogService.CREATE_EXPENSE,
                    String.valueOf(response.data().expenseId()),
                    toAuditDetails(Map.of(
                            "amount", expense.getAmount(),
                            "description", expense.getDescription(),
                            "expenseCategory", expense.getExpenseCategory() != null
                                    ? expense.getExpenseCategory().name()
                                    : "",
                            "loggedByEmployeeId", expense.getLoggedByEmployeeId() != null
                                    ? expense.getLoggedByEmployeeId()
                                    : ""
                    )));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/v1/transactions/isp-settlement")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StandardResponse_SettlementCreated> logIspSettlement(@RequestBody IspSettlement settlement) {
        StandardResponse_SettlementCreated response = dailyLedgerService.saveIspSettlement(settlement);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/transactions/daily-summary")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StandardResponse_DailyCashSummaryData> getDailySummary(
            @RequestParam("targetDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {

        StandardResponse_DailyCashSummaryData response = dailyLedgerService.getDailySummary(targetDate);
        return EtagSupport.respondWithEtag(ifNoneMatch, response.data(), () -> ResponseEntity.ok(response));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", "ERROR");
        body.put("error", ex.getMessage());
        body.put("data", null);
        return ResponseEntity.badRequest().body(body);
    }

    private String toAuditDetails(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
