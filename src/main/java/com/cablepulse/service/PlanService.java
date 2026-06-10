package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.CreatePlanRequestDto;
import com.cablepulse.model.ConnectionProvider;
import com.cablepulse.model.GlobalPlan;
import com.cablepulse.repository.ConnectionProviderRepository;
import com.cablepulse.repository.GlobalPlanRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PlanService {

    private final GlobalPlanRepository globalPlanRepository;
    private final ConnectionProviderRepository connectionProviderRepository;

    public PlanService(
            GlobalPlanRepository globalPlanRepository,
            ConnectionProviderRepository connectionProviderRepository) {
        this.globalPlanRepository = globalPlanRepository;
        this.connectionProviderRepository = connectionProviderRepository;
    }

    @Transactional
    public GlobalPlan createPlan(CreatePlanRequestDto requestDto) {
        ConnectionProvider provider = connectionProviderRepository.findByName(requestDto.provider().trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Provider category not found: " + requestDto.provider()));

        GlobalPlan plan = new GlobalPlan();
        plan.setPlanId("plan-" + UUID.randomUUID());
        plan.setPlanName(requestDto.name().trim());
        plan.setMonthlyRate(BigDecimal.valueOf(requestDto.price()));
        plan.setProvider(provider);

        if (requestDto.channelsText() != null) {
            String channelsText = requestDto.channelsText().trim();
            plan.setChannelsText(channelsText.isEmpty() ? null : channelsText);
        }

        if (requestDto.isHd() != null) {
            plan.setHd(requestDto.isHd());
        }

        return globalPlanRepository.save(plan);
    }

    @Transactional
    public void deletePlan(String planId) {
        GlobalPlan plan = globalPlanRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found: " + planId));
        globalPlanRepository.delete(plan);
    }
}
