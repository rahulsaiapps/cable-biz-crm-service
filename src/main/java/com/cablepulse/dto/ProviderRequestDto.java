package com.cablepulse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Unified body for {@code POST /api/v1/workspace/providers}.
 *
 * <ul>
 *   <li>Territory onboarding: {@code location_name} (+ optional {@code blocks})</li>
 *   <li>ISP plan category: {@code name} only</li>
 * </ul>
 */
public class ProviderRequestDto {

    @JsonProperty("location_name")
    @Size(max = 100, message = "Territory location name must not exceed 100 characters")
    private String locationName;

    private List<@Size(max = 100) String> blocks;

    @Size(max = 50, message = "Provider name must not exceed 50 characters")
    private String name;

    public ProviderRequestDto() {}

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isTerritoryRequest() {
        return locationName != null && !locationName.isBlank();
    }
}
