package com.boticario.modules.sales.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateSaleRequest(
        /** Si se informa, tiene prioridad sobre customerName: el nombre del
         *  cliente se toma (snapshot) del Customer encontrado, no del texto
         *  libre. Sin customerId la venta sigue funcionando como en la Fase 6
         *  (mostrador anonimo, con o sin customerName libre). */
        Long customerId,
        String customerName,
        @NotEmpty(message = "una venta debe tener al menos un item") @Valid List<SaleItemRequest> items,
        @NotEmpty(message = "una venta debe tener al menos un pago") @Valid List<SalePaymentRequest> payments
) {
}
