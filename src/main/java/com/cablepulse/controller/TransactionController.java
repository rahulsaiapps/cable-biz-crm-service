package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/sync")
public class TransactionController {

    private final com.cablepulse.service.SyncService syncService;

    public TransactionController(com.cablepulse.service.SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/synchronize")
    public ResponseEntity<StandardResponse_SyncResolution> synchronizeOfflineQueue(
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId,
            @RequestBody SyncRequestPayload payload) {

        StandardResponse_SyncResolution response = syncService.processSyncBatch(payload);
        return ResponseEntity.ok(response);
    }
}
