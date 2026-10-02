package com.boticario.modules.sales.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaleItemRequest(
        @NotNull Long presentationId,
        @NotNull @DecimalMin(value = "0.000001", message = "la cantidad debe ser mayor que cero") BigDecimal quantityPresentation,
        /** Solo ADMIN/FARMACEUTICO pueden enviar un valor mayor que cero (regla confirmada: CAJERO no aplica descuento). */
        @DecimalMin(value = "0", message = "el descuento no puede ser negativo")
        @DecimalMax(value = "100", message = "el descuento no puede superar 100%")
        BigDecimal discountPercentage
) {
}
