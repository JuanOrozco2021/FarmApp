package com.boticario.modules.purchasing.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Una linea al crear o reemplazar los items de una compra en BORRADOR. */
public record PurchaseItemRequest(
        @NotNull Long presentationId,
        @NotNull @DecimalMin(value = "0.000001", message = "la cantidad debe ser mayor que cero") BigDecimal quantityPresentation,
        @NotNull @DecimalMin(value = "0", message = "el costo unitario no puede ser negativo") BigDecimal unitCost,
        /** Solo aplica si el producto controla lote; si se omite se genera uno automatico al recibir. */
        String lotCode,
        LocalDate expirationDate
) {
}
