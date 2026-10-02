package com.boticario.modules.injections.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelInjectionRequest(
        @NotBlank(message = "el motivo es obligatorio para anular un registro") String reason
) {
}
