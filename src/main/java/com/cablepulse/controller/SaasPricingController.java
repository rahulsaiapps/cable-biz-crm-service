package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.ApplicationSubscriptionTier;
import com.cablepulse.model.SubscriptionUpgradeIntent;
import com.cablepulse.repository.ApplicationSubscriptionTierRepository;
import com.cablepulse.repository.OperatorCompanyRepository;
import com.cablepulse.repository.SubscriptionUpgradeIntentRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/saas")
public class SaasPricingController {

    private static final Logger logger = LoggerFactory.getLogger(SaasPricingController.class);

    private static final long PROMOTIONAL_TRIAL_TENANT_LIMIT = 100;
    private static final String DEFAULT_CURRENCY_CODE = "INR";

    private final ApplicationSubscriptionTierRepository subscriptionTierRepository;
    private final OperatorCompanyRepository operatorCompanyRepository;
    private final SubscriptionUpgradeIntentRepository upgradeIntentRepository;

    public SaasPricingController(ApplicationSubscriptionTierRepository subscriptionTierRepository,
                                 OperatorCompanyRepository operatorCompanyRepository,
                                 SubscriptionUpgradeIntentRepository upgradeIntentRepository) {
        this.subscriptionTierRepository = subscriptionTierRepository;
        this.operatorCompanyRepository = operatorCompanyRepository;
        this.upgradeIntentRepository = upgradeIntentRepository;
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

    @PostMapping("/upgrade-intent")
    public ResponseEntity<StandardResponse_UpgradeIntentData> recordUpgradeIntent(
            @Valid @RequestBody UpgradeIntentRequestDto requestDto) {

        String employeeId = SecurityContextHolder.getContext().getAuthentication().getName();

        SubscriptionUpgradeIntent intent = new SubscriptionUpgradeIntent(
                requestDto.tierName(),
                requestDto.billingCycle(),
                requestDto.amount(),
                employeeId
        );
        SubscriptionUpgradeIntent saved = upgradeIntentRepository.save(intent);

        logger.info("Upgrade intent recorded: employeeId={}, tier={}, billingCycle={}, amount={}",
                employeeId, saved.getTierName(), saved.getBillingCycle(), saved.getAmount());

        StandardResponse_UpgradeIntentData response = new StandardResponse_UpgradeIntentData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                new UpgradeIntentData(saved.getId())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
