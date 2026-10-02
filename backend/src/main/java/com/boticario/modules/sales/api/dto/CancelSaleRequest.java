package com.boticario.modules.sales.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelSaleRequest(
        @NotBlank(message = "el motivo es obligatorio para anular una venta") String reason
) {
}
