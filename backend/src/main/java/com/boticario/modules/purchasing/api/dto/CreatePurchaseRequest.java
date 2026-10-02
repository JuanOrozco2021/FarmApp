package com.boticario.modules.purchasing.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseRequest(
        @NotNull Long supplierId,
        String invoiceNumber,
        LocalDate purchaseDate,
        String notes,
        @NotEmpty(message = "una compra debe tener al menos un item") @Valid List<PurchaseItemRequest> items
) {
}
