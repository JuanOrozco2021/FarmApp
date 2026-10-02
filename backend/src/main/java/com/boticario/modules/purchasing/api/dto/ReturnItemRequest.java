package com.boticario.modules.purchasing.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Devolucion (parcial o total) de un item ya recibido, hacia el proveedor. */
public record ReturnItemRequest(
        @NotNull @DecimalMin(value = "0.000001", message = "la cantidad a devolver debe ser mayor que cero") BigDecimal quantityBase,
        @NotBlank(message = "el motivo es obligatorio para toda devolucion a proveedor") String reason
) {
}
