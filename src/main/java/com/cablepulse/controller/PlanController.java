package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.GlobalPlan;
import com.cablepulse.repository.GlobalPlanRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final GlobalPlanRepository globalPlanRepository;

    public PlanController(GlobalPlanRepository globalPlanRepository) {
        this.globalPlanRepository = globalPlanRepository;
    }

    @GetMapping
    public ResponseEntity<StandardResponse_PlansData> getPlansByProvider(
            @RequestParam("providerName") String providerName) {

        List<GlobalPlan> plans = globalPlanRepository.findByProvider_Name(providerName);

        List<PlanItemDTO> dtos = plans.stream()
                .map(p -> new PlanItemDTO(
                        p.getPlanId(),
                        p.getPlanName(),
                        p.getMonthlyRate(),
                        p.getFeatures() != null ? String.join(", ", p.getFeatures()) : ""
                ))
                .collect(Collectors.toList());

        StandardResponse_PlansData response = new StandardResponse_PlansData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                dtos
        );

        return ResponseEntity.ok(response);
    }
}
