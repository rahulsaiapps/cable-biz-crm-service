package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.dto.ProviderRequestDto;
import com.cablepulse.model.Customer;
import com.cablepulse.model.ConnectionProvider;
import com.cablepulse.model.Territory;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.TerritoryRepository;
import com.cablepulse.repository.ConnectionProviderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceController {

    private final CustomerRepository customerRepository;
    private final TerritoryRepository territoryRepository;
    private final ConnectionProviderRepository connectionProviderRepository;

    public WorkspaceController(CustomerRepository customerRepository, 
                               TerritoryRepository territoryRepository,
                               ConnectionProviderRepository connectionProviderRepository) {
        this.customerRepository = customerRepository;
        this.territoryRepository = territoryRepository;
        this.connectionProviderRepository = connectionProviderRepository;
    }

    @GetMapping("/customers")
    public ResponseEntity<StandardResponse_WorkspaceData> getWorkspaceCustomers(
            @RequestParam("locationId") String locationId,
            @RequestHeader("X-E2E-ID") UUID e2eId,
            @RequestHeader("X-Session-ID") UUID sessionId) {

        String locationName = territoryRepository.findById(locationId)
                .map(t -> t.getLocationName())
                .orElse("Default Location");

        List<Customer> customers = customerRepository.findByTerritory_TerritoryId(locationId);

        List<WorkspaceCustomerDTO> dtos = customers.stream().map(c -> {
            String planName = c.getGlobalPlan() != null ? c.getGlobalPlan().getPlanName() : "No Plan";
            BigDecimal rate = c.getCustomRateOverride() != null ? c.getCustomRateOverride() :
                    (c.getGlobalPlan() != null ? c.getGlobalPlan().getMonthlyRate() : BigDecimal.ZERO);

            return new WorkspaceCustomerDTO(
                    c.getCustomerId(),
                    c.getSerialNumber(),
                    c.getFullName(),
                    c.getDoorNumber(),
                    c.getBlockName(), // Map blockName to streetName field in the DTO
                    planName,
                    rate,
                    "UNPAID", // Default status
                    rate,     // Default balance due equal to rate
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

        // Triggers the @SQLDelete soft-delete UPDATE instead of a hard DELETE
        territoryRepository.delete(territory);

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
    public ResponseEntity<StandardResponse_Provider> createProvider(@Valid @RequestBody ProviderRequestDto requestDto) {
        ConnectionProvider provider = new ConnectionProvider(requestDto.getName());
        ConnectionProvider saved = connectionProviderRepository.save(provider);

        StandardResponse_Provider response = new StandardResponse_Provider(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                saved
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
}
