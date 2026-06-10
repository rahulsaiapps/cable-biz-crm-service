package com.cablepulse.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "global_plans")
public class GlobalPlan {

    @Id
    @Column(name = "plan_id")
    private String planId;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(name = "monthly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyRate;

    @Column(name = "channels_text", columnDefinition = "text")
    private String channelsText;

    @Column(name = "is_hd")
    private Boolean hd = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private ConnectionProvider provider;

    public GlobalPlan() {}

    public GlobalPlan(String planId, String planName, BigDecimal monthlyRate, String channelsText) {
        this.planId = planId;
        this.planName = planName;
        this.monthlyRate = monthlyRate;
        this.channelsText = channelsText;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public BigDecimal getMonthlyRate() {
        return monthlyRate;
    }

    public void setMonthlyRate(BigDecimal monthlyRate) {
        this.monthlyRate = monthlyRate;
    }

    public ConnectionProvider getProvider() {
        return provider;
    }

    public void setProvider(ConnectionProvider provider) {
        this.provider = provider;
    }

    public String getChannelsText() {
        return channelsText;
    }

    public void setChannelsText(String channelsText) {
        this.channelsText = channelsText;
    }

    public Boolean getHd() {
        return hd;
    }

    public void setHd(Boolean hd) {
        this.hd = hd;
    }
}
