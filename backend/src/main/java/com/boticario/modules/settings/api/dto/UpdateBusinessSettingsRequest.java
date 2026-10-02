package com.boticario.modules.settings.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateBusinessSettingsRequest(
        @NotBlank String pharmacyName,
        String nit,
        String address,
        @Min(value = 1, message = "los dias de alerta deben ser al menos 1") Integer expirationAlertDays
) {
}
