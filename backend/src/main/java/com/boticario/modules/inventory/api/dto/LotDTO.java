package com.boticario.modules.inventory.api.dto;

import com.boticario.modules.inventory.domain.InventoryLot;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LotDTO(
        Long id,
        Long productId,
        String productName,
        String lotCode,
        LocalDate expirationDate,
        BigDecimal quantityBase,
        boolean active,
        boolean expired,
        boolean expiringSoon
) {
    public static LotDTO from(InventoryLot lot, LocalDate today, LocalDate alertCutoff) {
        boolean expired = lot.getExpirationDate() != null && !lot.getExpirationDate().isAfter(today);
        boolean expiringSoon = !expired && lot.getExpirationDate() != null && !lot.getExpirationDate().isAfter(alertCutoff);
        return new LotDTO(lot.getId(), lot.getProduct().getId(), lot.getProduct().getName(), lot.getLotCode(),
                lot.getExpirationDate(), lot.getQuantityBase(), lot.isActive(), expired, expiringSoon);
    }
}
