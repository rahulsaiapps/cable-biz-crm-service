package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.dto.ProviderRequestDto;
import com.cablepulse.exception.ProviderCategoryAlreadyExistsException;
import com.cablepulse.exception.TerritoryAlreadyExistsException;
import com.cablepulse.model.ConnectionProvider;
import com.cablepulse.model.Customer;
import com.cablepulse.model.Territory;
import com.cablepulse.repository.ConnectionProviderRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.TerritoryRepository;
import com.cablepulse.service.AuditLogService;
import com.cablepulse.service.CustomerBalanceService;
import com.cablepulse.service.TerritoryService;
import com.cablepulse.service.WorkspaceProviderService;
import com.cablepulse.security.SecurityAuth;
import com.cablepulse.security.WorkspaceAuthorizationService;
import com.cablepulse.service.WorkspaceService;
import com.cablepulse.util.EtagSupport;
import com.cablepulse.util.PiiMaskingUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceController {

    private final CustomerRepository customerRepository;
    private final TerritoryRepository territoryRepository;
    private final ConnectionProviderRepository connectionProviderRepository;
    private final WorkspaceProviderService workspaceProviderService;
    private final CustomerBalanceService customerBalanceService;
    private final TerritoryService territoryService;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WorkspaceService workspaceService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public WorkspaceController(
            CustomerRepository customerRepository,
            TerritoryRepository territoryRepository,
            ConnectionProviderRepository connectionProviderRepository,
            WorkspaceProviderService workspaceProviderService,
            CustomerBalanceService customerBalanceService,
            TerritoryService territoryService,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            WorkspaceService workspaceService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper) {
        this.customerRepository = customerRepository;
        this.territoryRepository = territoryRepository;
        this.connectionProviderRepository = connectionProviderRepository;
        this.workspaceProviderService = workspaceProviderService;
        this.customerBalanceService = customerBalanceService;
        this.territoryService = territoryService;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.workspaceService = workspaceService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/info")
    public ResponseEntity<StandardResponse_WorkspaceInfo> getWorkspaceInfo() {
        String workspaceId = SecurityAuth.requireWorkspaceId();
        String businessName = workspaceService.businessNameFor(workspaceId);
        WorkspaceInfo info = new WorkspaceInfo(workspaceId, businessName);
        StandardResponse_WorkspaceInfo response = new StandardResponse_WorkspaceInfo(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                info
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/business-name")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StandardResponse_WorkspaceInfo> updateBusinessName(
            @RequestBody UpdateBusinessNameRequest request) {
        String workspaceId = SecurityAuth.requireWorkspaceId();
        var workspace = workspaceService.updateBusinessName(workspaceId, request.businessName());
        WorkspaceInfo info = new WorkspaceInfo(workspace.getWorkspaceId(), workspace.getBusinessName());
        StandardResponse_WorkspaceInfo response = new StandardResponse_WorkspaceInfo(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                info
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/territories")
    public ResponseEntity<StandardResponse_Territories> listTerritories(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        String workspaceId = SecurityAuth.requireWorkspaceId();
        List<TerritorySummaryDTO> summaries = workspaceAuthorizationService
                .filterAccessibleTerritories(territoryRepository.findByWorkspaceId(workspaceId)).stream()
                .map(this::toTerritorySummary)
                .collect(Collectors.toList());

        return EtagSupport.respondWithEtag(ifNoneMatch, summaries, () -> {
            StandardResponse_Territories response = new StandardResponse_Territories(
                    LocalDateTime.now(),
                    "SUCCESS",
                    null,
                    summaries
            );
            return ResponseEntity.ok(response);
        });
    }

    @GetMapping("/territories/{id}/blocks")
    public ResponseEntity<StandardResponse_BlockNames> getTerritoryBlocks(
            @PathVariable("id") String id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        workspaceAuthorizationService.assertTerritoryAccess(id);

        Territory territory = territoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Territory not found: " + id));

        List<String> blockNames = territory.getBlocks().stream()
                .map(block -> block.getBlockName())
                .collect(Collectors.toList());

        return EtagSupport.respondWithEtag(ifNoneMatch, blockNames, () -> {
            StandardResponse_BlockNames response = new StandardResponse_BlockNames(
                    LocalDateTime.now(),
                    "SUCCESS",
                    null,
                    blockNames
            );
            return ResponseEntity.ok(response);
        });
    }

    @GetMapping("/territories/active-locations")
    public ResponseEntity<StandardResponse_LocationNames> getActiveTerritoryLocationNames(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        String workspaceId = SecurityAuth.requireWorkspaceId();
        Set<String> allowedLocationNames = workspaceAuthorizationService
                .filterAccessibleTerritories(territoryRepository.findByWorkspaceId(workspaceId)).stream()
                .map(Territory::getLocationName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());

        List<String> locationNames = territoryRepository.findDistinctActiveLocationNames(workspaceId).stream()
                .filter(allowedLocationNames::contains)
                .collect(Collectors.toList());

        return EtagSupport.respondWithEtag(ifNoneMatch, locationNames, () -> {
            StandardResponse_LocationNames response = new StandardResponse_LocationNames(
                    LocalDateTime.now(),
                    "SUCCESS",
                    null,
                    locationNames
            );
            return ResponseEntity.ok(response);
        });
    }

    @GetMapping("/customers")
    public ResponseEntity<StandardResponse_WorkspaceData> getWorkspaceCustomers(
            @RequestParam("locationId") String locationId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        workspaceAuthorizationService.assertTerritoryAccess(locationId);

        String locationName = territoryRepository.findById(locationId)
                .map(t -> t.getLocationName())
                .orElse("Default Location");

        List<Customer> customers = customerRepository.findByTerritoryIdWithPlan(
                SecurityAuth.requireWorkspaceId(), locationId);
        List<String> customerIds = customers.stream().map(Customer::getCustomerId).toList();
        Map<String, BigDecimal> balanceByCustomerId = customerBalanceService.sumDueAmountByCustomerIds(customerIds);

        List<WorkspaceCustomerDTO> dtos = customers.stream().map(c -> {
            String planName = c.getGlobalPlan() != null ? c.getGlobalPlan().getPlanName() : "No Plan";
            BigDecimal rate = c.getCustomRateOverride() != null ? c.getCustomRateOverride() :
                    (c.getGlobalPlan() != null ? c.getGlobalPlan().getMonthlyRate() : BigDecimal.ZERO);
            BigDecimal balanceDue = balanceByCustomerId.getOrDefault(c.getCustomerId(), BigDecimal.ZERO);
            String paymentStatus = CustomerBalanceService.paymentStatusFromBalance(balanceDue);

            String boxNumber = c.getBoxNumber();
            String cardNumber = c.getCardNumber();
            if (!workspaceAuthorizationService.canViewSensitiveCustomerFields()) {
                boxNumber = PiiMaskingUtil.maskPhone(boxNumber);
                cardNumber = PiiMaskingUtil.maskPhone(cardNumber);
            }

            return new WorkspaceCustomerDTO(
                    c.getCustomerId(),
                    c.getSerialNumber(),
                    c.getFullName(),
                    c.getDoorNumber(),
                    c.getBlockName(),
                    planName,
                    rate,
                    paymentStatus,
                    balanceDue,
                    c.getConnectionType(),
                    boxNumber,
                    cardNumber
            );
        }).collect(Collectors.toList());

        WorkspaceData data = new WorkspaceData(locationId, locationName, dtos);

        return EtagSupport.respondWithEtag(ifNoneMatch, data, () -> {
            StandardResponse_WorkspaceData response = new StandardResponse_WorkspaceData(
                    LocalDateTime.now(),
                    "SUCCESS",
                    null,
                    data
            );
            return ResponseEntity.ok(response);
        });
    }

    @DeleteMapping("/territories/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StandardResponse_Void> deleteTerritory(
            @PathVariable("id") String id,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        Territory territory = territoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Territory not found: " + id));

        territoryService.softDeleteTerritory(territory.getTerritoryId());

        auditLogService.log(
                AuditLogService.DELETE_TERRITORY,
                id,
                toAuditDetails(Map.of(
                        "locationName", territory.getLocationName(),
                        "district", territory.getDistrict() != null ? territory.getDistrict() : "",
                        "state", territory.getState() != null ? territory.getState() : ""
                )));

        StandardResponse_Void response = new StandardResponse_Void(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                null
        );
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<StandardResponse_Void> handleNotFound(EntityNotFoundException ex) {
        StandardResponse_Void response = new StandardResponse_Void(
                LocalDateTime.now(),
                "ERROR",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @GetMapping("/providers")
    public ResponseEntity<StandardResponse_Providers> getProviders(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        List<ConnectionProvider> providers =
                connectionProviderRepository.findByWorkspaceId(SecurityAuth.requireWorkspaceId());
        List<ProviderSummaryDTO> etagSource = providers.stream()
                .map(p -> new ProviderSummaryDTO(p.getId(), p.getName()))
                .collect(Collectors.toList());

        return EtagSupport.respondWithEtag(ifNoneMatch, etagSource, () -> {
            StandardResponse_Providers response = new StandardResponse_Providers(
                    LocalDateTime.now(),
                    "SUCCESS",
                    null,
                    providers
            );
            return ResponseEntity.ok(response);
        });
    }

    @PostMapping("/providers")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> createProvider(@Valid @RequestBody ProviderRequestDto requestDto) {
        Object saved = workspaceProviderService.createWorkspaceProvider(requestDto);

        if (saved instanceof Territory territory) {
            StandardResponse_Territory response = new StandardResponse_Territory(
                    LocalDateTime.now(),
                    "SUCCESS",
                    null,
                    toTerritorySummary(territory)
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        ConnectionProvider provider = (ConnectionProvider) saved;
        StandardResponse_Provider response = new StandardResponse_Provider(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                provider
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(ProviderCategoryAlreadyExistsException.class)
    public ResponseEntity<StandardResponse_Provider> handleProviderCategoryAlreadyExists(
            ProviderCategoryAlreadyExistsException ex) {
        StandardResponse_Provider response = new StandardResponse_Provider(
                LocalDateTime.now(),
                "ERROR",
                ex.getMessage(),
                ex.getExisting()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(TerritoryAlreadyExistsException.class)
    public ResponseEntity<StandardResponse_Territory> handleTerritoryAlreadyExists(
            TerritoryAlreadyExistsException ex) {
        StandardResponse_Territory response = new StandardResponse_Territory(
                LocalDateTime.now(),
                "ERROR",
                ex.getMessage(),
                toTerritorySummary(ex.getExisting())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    private TerritorySummaryDTO toTerritorySummary(Territory territory) {
        String territoryId = territory.getTerritoryId();
        long customerCount = customerRepository.countByTerritory_TerritoryId(territoryId);
        long activeCount = customerRepository.countPaidCustomersByTerritoryId(territoryId);
        long pendingCount = customerRepository.countPendingCustomersByTerritoryId(territoryId);
        return new TerritorySummaryDTO(
                territoryId,
                territory.getLocationName(),
                customerCount,
                activeCount,
                pendingCount
        );
    }

    private String toAuditDetails(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    public record StandardResponse_Providers(
        LocalDateTime timestamp,
        String status,
        String error,
        List<ConnectionProvider> data
    ) {}

    public record StandardResponse_Provider(
        LocalDateTime timestamp,
        String status,
        String error,
        ConnectionProvider data
    ) {}

    public record StandardResponse_Void(
        LocalDateTime timestamp,
        String status,
        String error,
        Void data
    ) {}

    public record StandardResponse_BlockNames(
        LocalDateTime timestamp,
        String status,
        String error,
        List<String> data
    ) {}

    public record ProviderSummaryDTO(Long id, String name) {}

    public record WorkspaceInfo(String workspaceId, String businessName) {}

    public record UpdateBusinessNameRequest(String businessName) {}

    public record StandardResponse_WorkspaceInfo(
            LocalDateTime timestamp,
            String status,
            String error,
            WorkspaceInfo data
    ) {}
}
