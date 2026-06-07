package com.cablepulse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "operator_companies")
public class OperatorCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "promotional_trial_active")
    private Boolean promotionalTrialActive = false;

    @Column(name = "trial_end_date")
    private LocalDateTime trialEndDate;

    @Column(name = "current_billing_amount")
    private Double currentBillingAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_subscription_tier_id")
    private ApplicationSubscriptionTier activeSubscriptionTier;

    public OperatorCompany() {}

    public OperatorCompany(String companyName, Boolean promotionalTrialActive, LocalDateTime trialEndDate, Double currentBillingAmount, ApplicationSubscriptionTier activeSubscriptionTier) {
        this.companyName = companyName;
        this.promotionalTrialActive = promotionalTrialActive;
        this.trialEndDate = trialEndDate;
        this.currentBillingAmount = currentBillingAmount;
        this.activeSubscriptionTier = activeSubscriptionTier;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Boolean getPromotionalTrialActive() {
        return promotionalTrialActive;
    }

    public void setPromotionalTrialActive(Boolean promotionalTrialActive) {
        this.promotionalTrialActive = promotionalTrialActive;
    }

    public LocalDateTime getTrialEndDate() {
        return trialEndDate;
    }

    public void setTrialEndDate(LocalDateTime trialEndDate) {
        this.trialEndDate = trialEndDate;
    }

    public Double getCurrentBillingAmount() {
        return currentBillingAmount;
    }

    public void setCurrentBillingAmount(Double currentBillingAmount) {
        this.currentBillingAmount = currentBillingAmount;
    }

    public ApplicationSubscriptionTier getActiveSubscriptionTier() {
        return activeSubscriptionTier;
    }

    public void setActiveSubscriptionTier(ApplicationSubscriptionTier activeSubscriptionTier) {
        this.activeSubscriptionTier = activeSubscriptionTier;
    }
}
