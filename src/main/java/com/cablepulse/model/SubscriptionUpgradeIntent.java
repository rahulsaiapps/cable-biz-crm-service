package com.cablepulse.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_upgrade_intents")
public class SubscriptionUpgradeIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tier_name", nullable = false)
    private String tierName;

    @Column(name = "billing_cycle", nullable = false)
    private String billingCycle;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "requested_by_employee_id", nullable = false)
    private String requestedByEmployeeId;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    public SubscriptionUpgradeIntent() {}

    public SubscriptionUpgradeIntent(String tierName, String billingCycle, Double amount, String requestedByEmployeeId) {
        this.tierName = tierName;
        this.billingCycle = billingCycle;
        this.amount = amount;
        this.requestedByEmployeeId = requestedByEmployeeId;
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

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getRequestedByEmployeeId() {
        return requestedByEmployeeId;
    }

    public void setRequestedByEmployeeId(String requestedByEmployeeId) {
        this.requestedByEmployeeId = requestedByEmployeeId;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
