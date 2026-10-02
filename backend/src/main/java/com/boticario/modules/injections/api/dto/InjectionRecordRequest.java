package com.boticario.modules.injections.api.dto;

import com.boticario.modules.sales.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InjectionRecordRequest(
        /** Si se informa, tiene prioridad sobre patientName: el nombre se toma
         *  (snapshot) del Customer encontrado. Sin customerId, patientName es
         *  obligatorio — a diferencia de una venta, un servicio medico no
         *  puede quedar completamente anonimo. */
        Long customerId,
        String patientName,
        @NotBlank(message = "el nombre del medicamento es obligatorio") String medicationName,
        String dose,
        String lotCode,
        String applicationSite,
        Long relatedSaleId,
        String observations,
        @NotNull @DecimalMin(value = "0", message = "la tarifa no puede ser negativa") BigDecimal fee,
        @NotNull(message = "la forma de pago es obligatoria") PaymentMethod paymentMethod
) {
}
