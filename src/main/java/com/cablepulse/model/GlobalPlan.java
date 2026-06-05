package com.cablepulse.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.List;

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

    @Column(name = "features", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> features;

    public GlobalPlan() {}

    public GlobalPlan(String planId, String planName, BigDecimal monthlyRate, List<String> features) {
        this.planId = planId;
        this.planName = planName;
        this.monthlyRate = monthlyRate;
        this.features = features;
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

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }
}
