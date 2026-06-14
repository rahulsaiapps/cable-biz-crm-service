package com.cablepulse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddTerritoryBlockRequest {

    @NotBlank(message = "Block name is required and cannot be blank")
    @Size(max = 100, message = "Block name must not exceed 100 characters")
    @JsonProperty("block_name")
    private String blockName;

    public AddTerritoryBlockRequest() {}

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }
}
