package com.cablepulse.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "customers")
@SQLDelete(sql = "UPDATE customers SET is_deleted = true WHERE customer_id = ?")
@SQLRestriction("is_deleted = false")
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

    @Column(name = "connection_type")
    private String connectionType;

    @Column(name = "box_number")
    private String boxNumber;

    @Column(name = "card_number")
    private String cardNumber;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

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

    public Customer(String customerId, int serialNumber, String fullName, String mobileNumber, String blockName, String doorNumber, BigDecimal customRateOverride, Territory territory, GlobalPlan globalPlan, String connectionType, String boxNumber, String cardNumber) {
        this.customerId = customerId;
        this.serialNumber = serialNumber;
        this.fullName = fullName;
        this.mobileNumber = mobileNumber;
        this.blockName = blockName;
        this.doorNumber = doorNumber;
        this.customRateOverride = customRateOverride;
        this.territory = territory;
        this.globalPlan = globalPlan;
        this.connectionType = connectionType;
        this.boxNumber = boxNumber;
        this.cardNumber = cardNumber;
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

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getBoxNumber() {
        return boxNumber;
    }

    public void setBoxNumber(String boxNumber) {
        this.boxNumber = boxNumber;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
