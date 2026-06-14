package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.CreatePlanRequestDto;
import com.cablepulse.dto.DtoClasses.UpdatePlanRequestDto;
import com.cablepulse.model.ConnectionProvider;
import com.cablepulse.model.GlobalPlan;
import com.cablepulse.repository.ConnectionProviderRepository;
import com.cablepulse.repository.GlobalPlanRepository;
import com.cablepulse.security.SecurityAuth;
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
        String workspaceId = SecurityAuth.requireWorkspaceId();
        ConnectionProvider provider = connectionProviderRepository
                .findByWorkspaceIdAndNameIgnoreCase(workspaceId, requestDto.provider().trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Provider category not found: " + requestDto.provider()));

        GlobalPlan plan = new GlobalPlan();
        plan.setPlanId("plan-" + UUID.randomUUID());
        plan.setPlanName(requestDto.name().trim());
        plan.setMonthlyRate(BigDecimal.valueOf(requestDto.price()));
        plan.setProvider(provider);
        plan.setWorkspaceId(workspaceId);

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
        String workspaceId = SecurityAuth.requireWorkspaceId();
        GlobalPlan plan = globalPlanRepository.findByPlanIdAndWorkspaceId(planId, workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found: " + planId));
        globalPlanRepository.delete(plan);
    }

    @Transactional
    public GlobalPlan updatePlan(String planId, UpdatePlanRequestDto requestDto) {
        String workspaceId = SecurityAuth.requireWorkspaceId();
        GlobalPlan plan = globalPlanRepository.findByPlanIdAndWorkspaceId(planId, workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found: " + planId));
        plan.setPlanName(requestDto.name().trim());
        plan.setMonthlyRate(BigDecimal.valueOf(requestDto.price()));
        return globalPlanRepository.save(plan);
    }
}
