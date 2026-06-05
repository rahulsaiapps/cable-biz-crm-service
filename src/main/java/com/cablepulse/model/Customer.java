package com.cablepulse.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "serial_number", nullable = false)
    private int serialNumber;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "block_name")
    private String blockName;

    @Column(name = "door_number")
    private String doorNumber;

    @Column(name = "custom_rate_override", precision = 10, scale = 2)
    private BigDecimal customRateOverride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_id")
    private Territory territory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private GlobalPlan globalPlan;

    public Customer() {}

    public Customer(String customerId, int serialNumber, String fullName, String mobileNumber, String blockName, String doorNumber, BigDecimal customRateOverride, Territory territory, GlobalPlan globalPlan) {
        this.customerId = customerId;
        this.serialNumber = serialNumber;
        this.fullName = fullName;
        this.mobileNumber = mobileNumber;
        this.blockName = blockName;
        this.doorNumber = doorNumber;
        this.customRateOverride = customRateOverride;
        this.territory = territory;
        this.globalPlan = globalPlan;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public String getDoorNumber() {
        return doorNumber;
    }

    public void setDoorNumber(String doorNumber) {
        this.doorNumber = doorNumber;
    }

    public BigDecimal getCustomRateOverride() {
        return customRateOverride;
    }

    public void setCustomRateOverride(BigDecimal customRateOverride) {
        this.customRateOverride = customRateOverride;
    }

    public Territory getTerritory() {
        return territory;
    }

    public void setTerritory(Territory territory) {
        this.territory = territory;
    }

    public GlobalPlan getGlobalPlan() {
        return globalPlan;
    }

    public void setGlobalPlan(GlobalPlan globalPlan) {
        this.globalPlan = globalPlan;
    }
}
