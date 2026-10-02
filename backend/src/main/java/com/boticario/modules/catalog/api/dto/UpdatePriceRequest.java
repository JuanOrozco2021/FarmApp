package com.boticario.modules.catalog.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdatePriceRequest(
        @NotNull @DecimalMin(value = "0", message = "el precio no puede ser negativo") BigDecimal newPrice,
        String reason
) {
}
