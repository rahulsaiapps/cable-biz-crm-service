package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.GlobalPlan;
import com.cablepulse.repository.GlobalPlanRepository;
import com.cablepulse.service.PlanService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final GlobalPlanRepository globalPlanRepository;
    private final PlanService planService;

    public PlanController(GlobalPlanRepository globalPlanRepository, PlanService planService) {
        this.globalPlanRepository = globalPlanRepository;
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<StandardResponse_PlansData> getPlansByProvider(
            @RequestParam(value = "providerName", required = false) String providerName) {

        List<GlobalPlan> plans = (providerName == null || providerName.isBlank())
                ? globalPlanRepository.findAllWithProvider()
                : globalPlanRepository.findByProvider_NameWithProvider(providerName.trim());

        List<PlanItemDTO> dtos = plans.stream()
                .map(PlanController::toPlanItemDto)
                .collect(Collectors.toList());

        StandardResponse_PlansData response = new StandardResponse_PlansData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                dtos
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<StandardResponse_PlanCreated> createPlan(
            @Valid @RequestBody CreatePlanRequestDto requestDto) {
        GlobalPlan saved = planService.createPlan(requestDto);

        StandardResponse_PlanCreated response = new StandardResponse_PlanCreated(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                new PlanCreatedData(saved.getPlanId())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StandardResponse_Void> deletePlan(@PathVariable("id") String id) {
        planService.deletePlan(id);

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

    private static PlanItemDTO toPlanItemDto(GlobalPlan plan) {
        String details = plan.getChannelsText() != null ? plan.getChannelsText() : "";
        String providerName = plan.getProvider() != null ? plan.getProvider().getName() : null;
        return new PlanItemDTO(
                plan.getPlanId(),
                plan.getPlanName(),
                plan.getMonthlyRate(),
                details,
                providerName
        );
    }

    public record StandardResponse_Void(
            LocalDateTime timestamp,
            String status,
            String error,
            Void data
    ) {}
}
