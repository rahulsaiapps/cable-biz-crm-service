package com.cablepulse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class TerritoryRequestDto {

    @NotBlank(message = "Territory location name is required and cannot be blank")
    @Size(max = 100, message = "Territory location name must not exceed 100 characters")
    @JsonProperty("location_name")
    private String locationName;

    private List<@NotBlank @Size(max = 100) String> blocks;

    public TerritoryRequestDto() {}

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public List<String> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<String> blocks) {
        this.blocks = blocks;
    }
}
