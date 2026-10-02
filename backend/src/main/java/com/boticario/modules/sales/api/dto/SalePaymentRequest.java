package com.boticario.modules.sales.api.dto;

import com.boticario.modules.sales.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SalePaymentRequest(
        @NotNull PaymentMethod paymentMethod,
        @NotNull @DecimalMin(value = "0.01", message = "el monto debe ser mayor que cero") BigDecimal amount
) {
}
