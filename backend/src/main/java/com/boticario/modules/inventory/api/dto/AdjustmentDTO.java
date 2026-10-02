package com.boticario.modules.inventory.api.dto;

import com.boticario.modules.inventory.domain.InventoryAdjustment;

import java.math.BigDecimal;
import java.time.Instant;

public record AdjustmentDTO(
        Long id,
        Long productId,
        String adjustmentType,
        BigDecimal quantityBase,
        String reason,
        Long movementId,
        Instant createdAt
) {
    public static AdjustmentDTO from(InventoryAdjustment adjustment) {
        return new AdjustmentDTO(
                adjustment.getId(),
                adjustment.getProduct().getId(),
                adjustment.getAdjustmentType().name(),
                adjustment.getQuantityBase(),
                adjustment.getReason(),
                adjustment.getMovement().getId(),
                adjustment.getCreatedAt()
        );
    }
}
