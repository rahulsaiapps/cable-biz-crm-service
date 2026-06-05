package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.Customer;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.TerritoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public WorkspaceController(CustomerRepository customerRepository, TerritoryRepository territoryRepository) {
        this.customerRepository = customerRepository;
        this.territoryRepository = territoryRepository;
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
                    rate      // Default balance due equal to rate
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
}
