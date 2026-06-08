package com.cablepulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProviderRequestDto {

    @NotBlank(message = "Provider name is required and cannot be blank")
    @Size(max = 50, message = "Provider name must not exceed 50 characters")
    private String name;

    public ProviderRequestDto() {}

    public ProviderRequestDto(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
