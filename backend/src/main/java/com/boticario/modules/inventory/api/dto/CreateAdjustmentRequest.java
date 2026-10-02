package com.boticario.modules.inventory.api.dto;

import com.boticario.modules.inventory.domain.AdjustmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAdjustmentRequest(
        @NotNull Long productId,
        @NotNull AdjustmentType adjustmentType,
        @NotNull @DecimalMin(value = "0.000001", message = "la cantidad debe ser mayor que cero") BigDecimal quantityBase,
        @NotBlank(message = "el motivo es obligatorio para todo ajuste de inventario") String reason,
        /** Solo para ajustes negativos/merma/vencimiento dirigidos a un lote conocido. Si se omite, se usa FEFO. */
        Long lotId,
        /** Solo aplica a ajustes positivos: en que lote entra (por defecto, lote GENERAL si el producto no controla lote). */
        String lotCode,
        java.time.LocalDate expirationDate
) {
}
