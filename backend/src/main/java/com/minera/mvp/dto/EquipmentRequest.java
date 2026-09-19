package com.minera.mvp.dto;

import jakarta.validation.constraints.NotBlank;

public record EquipmentRequest(
        @NotBlank String code,
        @NotBlank String name,
        String area,
        String type) {
}