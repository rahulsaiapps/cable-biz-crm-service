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
import com.cablepulse.service.CustomerBalanceService;
import com.cablepulse.service.TerritoryService;
import com.cablepulse.service.WorkspaceProviderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    public WorkspaceController(
            CustomerRepository customerRepository,
            TerritoryRepository territoryRepository,
            ConnectionProviderRepository connectionProviderRepository,
            WorkspaceProviderService workspaceProviderService,
            CustomerBalanceService customerBalanceService,
            TerritoryService territoryService) {
        this.customerRepository = customerRepository;
        this.territoryRepository = territoryRepository;
        this.connectionProviderRepository = connectionProviderRepository;
        this.workspaceProviderService = workspaceProviderService;
        this.customerBalanceService = customerBalanceService;
        this.territoryService = territoryService;
    }

    @GetMapping("/territories")
    public ResponseEntity<StandardResponse_Territories> listTerritories() {
        List<TerritorySummaryDTO> summaries = territoryRepository.findAll().stream()
                .map(this::toTerritorySummary)
                .collect(Collectors.toList());

        StandardResponse_Territories response = new StandardResponse_Territories(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                summaries
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/territories/{id}/blocks")
    public ResponseEntity<StandardResponse_BlockNames> getTerritoryBlocks(
            @PathVariable("id") String id,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        Territory territory = territoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Territory not found: " + id));

        List<String> blockNames = territory.getBlocks().stream()
                .map(block -> block.getBlockName())
                .collect(Collectors.toList());

        StandardResponse_BlockNames response = new StandardResponse_BlockNames(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                blockNames
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/territories/active-locations")
    public ResponseEntity<StandardResponse_LocationNames> getActiveTerritoryLocationNames() {
        List<String> locationNames = territoryRepository.findDistinctActiveLocationNames();

        StandardResponse_LocationNames response = new StandardResponse_LocationNames(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                locationNames
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/customers")
    public ResponseEntity<StandardResponse_WorkspaceData> getWorkspaceCustomers(
            @RequestParam("locationId") String locationId,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        String locationName = territoryRepository.findById(locationId)
                .map(t -> t.getLocationName())
                .orElse("Default Location");

        List<Customer> customers = customerRepository.findByTerritoryIdWithPlan(locationId);
        List<String> customerIds = customers.stream().map(Customer::getCustomerId).toList();
        Map<String, BigDecimal> balanceByCustomerId = customerBalanceService.sumDueAmountByCustomerIds(customerIds);

        List<WorkspaceCustomerDTO> dtos = customers.stream().map(c -> {
            String planName = c.getGlobalPlan() != null ? c.getGlobalPlan().getPlanName() : "No Plan";
            BigDecimal rate = c.getCustomRateOverride() != null ? c.getCustomRateOverride() :
                    (c.getGlobalPlan() != null ? c.getGlobalPlan().getMonthlyRate() : BigDecimal.ZERO);
            BigDecimal balanceDue = balanceByCustomerId.getOrDefault(c.getCustomerId(), BigDecimal.ZERO);
            String paymentStatus = CustomerBalanceService.paymentStatusFromBalance(balanceDue);

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
                    c.getBoxNumber(),
                    c.getCardNumber()
            );
        }).collect(Collectors.toList());

        WorkspaceData data = new WorkspaceData(locationId, locationName, dtos);
        StandardResponse_WorkspaceData response = new StandardResponse_WorkspaceData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                data
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/territories/{id}")
    public ResponseEntity<StandardResponse_Void> deleteTerritory(
            @PathVariable("id") String id,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        Territory territory = territoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Territory not found: " + id));

        territoryService.softDeleteTerritory(territory.getTerritoryId());

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
    public ResponseEntity<StandardResponse_Providers> getProviders() {
        List<ConnectionProvider> providers = connectionProviderRepository.findAll();
        StandardResponse_Providers response = new StandardResponse_Providers(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                providers
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/providers")
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
}
