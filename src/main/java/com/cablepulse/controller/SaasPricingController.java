package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.ApplicationSubscriptionTier;
import com.cablepulse.repository.ApplicationSubscriptionTierRepository;
import com.cablepulse.repository.OperatorCompanyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/saas")
public class SaasPricingController {

    private static final long PROMOTIONAL_TRIAL_TENANT_LIMIT = 100;
    private static final String DEFAULT_CURRENCY_CODE = "INR";

    private final ApplicationSubscriptionTierRepository subscriptionTierRepository;
    private final OperatorCompanyRepository operatorCompanyRepository;

    public SaasPricingController(ApplicationSubscriptionTierRepository subscriptionTierRepository,
                                 OperatorCompanyRepository operatorCompanyRepository) {
        this.subscriptionTierRepository = subscriptionTierRepository;
        this.operatorCompanyRepository = operatorCompanyRepository;
    }

    @GetMapping("/pricing")
    public ResponseEntity<StandardResponse_SaasPricingData> getSaasPricingMatrix(
            @RequestHeader(value = "X-User-Country-Code", required = false, defaultValue = "IN") String countryCode) {

        List<ApplicationSubscriptionTier> tiers = subscriptionTierRepository.findAll();

        List<SaasTierDTO> tierDTOs = tiers.stream()
                .map(t -> new SaasTierDTO(t.getTierName(), t.getBillingCycle(), t.getRetailPrice(), t.getDiscountedPrice()))
                .collect(Collectors.toList());

        String currencyCode = tiers.stream()
                .map(ApplicationSubscriptionTier::getCurrencyCode)
                .findFirst()
                .orElse(DEFAULT_CURRENCY_CODE);

        boolean promotionalTrialActive = operatorCompanyRepository.count() < PROMOTIONAL_TRIAL_TENANT_LIMIT;
        LocalDateTime trialEndsAt = promotionalTrialActive ? LocalDateTime.now().plusMonths(3) : null;

        SaasPricingData data = new SaasPricingData(promotionalTrialActive, trialEndsAt, currencyCode, tierDTOs);
        StandardResponse_SaasPricingData response = new StandardResponse_SaasPricingData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                data
        );

        return ResponseEntity.ok(response);
    }
}
