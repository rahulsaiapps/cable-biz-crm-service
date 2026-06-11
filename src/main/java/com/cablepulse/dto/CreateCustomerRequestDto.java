package com.cablepulse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/v1/customers}.
 *
 * Field names mirror the Flutter registration form payload.
 */
public class CreateCustomerRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    @JsonProperty("territory_id")
    private String territoryId;

    @JsonProperty("territory_name")
    private String territoryName;

    @JsonProperty("phone_number")
    private String phoneNumber;

    private String street;

    @JsonProperty("door_number")
    private String doorNumber;

    @JsonProperty("plan_name")
    private String planName;

    @PositiveOrZero
    @JsonProperty("plan_monthly_rate")
    private BigDecimal planMonthlyRate;

    @JsonProperty("box_number")
    private String boxNumber;

    @JsonProperty("card_number")
    private String cardNumber;

    @JsonProperty("connection_type")
    private String connectionType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTerritoryId() {
        return territoryId;
    }

    public void setTerritoryId(String territoryId) {
        this.territoryId = territoryId;
    }

    public String getTerritoryName() {
        return territoryName;
    }

    public void setTerritoryName(String territoryName) {
        this.territoryName = territoryName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getDoorNumber() {
        return doorNumber;
    }

    public void setDoorNumber(String doorNumber) {
        this.doorNumber = doorNumber;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public BigDecimal getPlanMonthlyRate() {
        return planMonthlyRate;
    }

    public void setPlanMonthlyRate(BigDecimal planMonthlyRate) {
        this.planMonthlyRate = planMonthlyRate;
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

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }
}
