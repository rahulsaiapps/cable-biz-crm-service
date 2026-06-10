package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.service.DailyLedgerService;
import com.cablepulse.service.FinanceAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {

    private final DailyLedgerService dailyLedgerService;
    private final FinanceAnalyticsService financeAnalyticsService;

    public FinanceController(DailyLedgerService dailyLedgerService, FinanceAnalyticsService financeAnalyticsService) {
        this.dailyLedgerService = dailyLedgerService;
        this.financeAnalyticsService = financeAnalyticsService;
    }

    @GetMapping("/daily-ledger")
    public ResponseEntity<StandardResponse_DailyLedgerBook> getDailyLedger(
            @RequestParam("targetDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        StandardResponse_DailyLedgerBook response = dailyLedgerService.getDailyLedgerBook(targetDate);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/daily-ledger/transactions")
    public ResponseEntity<StandardResponse_Void> recordDailyTransaction(
            @RequestBody RecordDailyTransactionRequestDto request,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        String agentId = SecurityContextHolder.getContext().getAuthentication().getName();
        dailyLedgerService.recordManualCollection(request, agentId);

        StandardResponse_Void response = new StandardResponse_Void(
                LocalDateTime.now(),
                "CREATED",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/metrics")
    public ResponseEntity<FinanceMetricsDTO> getFinanceMetrics(
            @RequestParam(value = "interval", defaultValue = "6M") String interval) {
        return ResponseEntity.ok(financeAnalyticsService.getMetrics(interval));
    }

    @GetMapping("/expenses")
    public ResponseEntity<List<ExpenseDistributionItemDTO>> getExpenseDistribution() {
        return ResponseEntity.ok(financeAnalyticsService.getExpenseDistribution());
    }

    @GetMapping("/performance")
    public ResponseEntity<List<MonthlyPerformanceDTO>> getMonthlyPerformance() {
        return ResponseEntity.ok(financeAnalyticsService.getMonthlyPerformance());
    }

    @GetMapping("/disbursements")
    public ResponseEntity<List<DisbursementDTO>> getDisbursements() {
        return ResponseEntity.ok(financeAnalyticsService.getRecentDisbursements());
    }

    @GetMapping("/health")
    public ResponseEntity<FinanceHealthDTO> getFinanceHealth() {
        return ResponseEntity.ok(financeAnalyticsService.getSystemHealth());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", "ERROR");
        body.put("error", ex.getMessage());
        body.put("data", null);
        return ResponseEntity.badRequest().body(body);
    }

    public record StandardResponse_Void(
            LocalDateTime timestamp,
            String status,
            String error,
            Void data
    ) {}
}
