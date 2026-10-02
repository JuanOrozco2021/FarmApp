package com.boticario.modules.catalog.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Presentacion a crear junto con el producto, o agregada despues a uno existente. */
public record PresentationRequest(
        @NotBlank String unitCode,
        @NotNull @DecimalMin(value = "0.000001", message = "el factor de conversion debe ser mayor que cero") BigDecimal conversionFactor,
        @NotNull @DecimalMin(value = "0", message = "el precio de venta no puede ser negativo") BigDecimal salePrice,
        BigDecimal purchasePrice,
        String barcode
) {
}
