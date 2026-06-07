package com.cablepulse.model;

import jakarta.persistence.*;

@Entity
@Table(name = "application_subscription_tiers")
public class ApplicationSubscriptionTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tier_name", nullable = false)
    private String tierName; // BASIC, PRO, ENTERPRISE

    @Column(name = "billing_cycle", nullable = false)
    private String billingCycle; // MONTHLY, YEARLY

    @Column(name = "retail_price", nullable = false)
    private Double retailPrice;

    @Column(name = "discounted_price", nullable = false)
    private Double discountedPrice;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode = "INR";

    public ApplicationSubscriptionTier() {}

    public ApplicationSubscriptionTier(String tierName, String billingCycle, Double retailPrice, Double discountedPrice, String currencyCode) {
        this.tierName = tierName;
        this.billingCycle = billingCycle;
        this.retailPrice = retailPrice;
        this.discountedPrice = discountedPrice;
        this.currencyCode = currencyCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }

    public Double getRetailPrice() {
        return retailPrice;
    }

    public void setRetailPrice(Double retailPrice) {
        this.retailPrice = retailPrice;
    }

    public Double getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(Double discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
