package com.boticario.modules.inventory.api.dto;

import com.boticario.modules.inventory.domain.InventoryMovement;

import java.math.BigDecimal;
import java.time.Instant;

public record MovementDTO(
        Long id,
        Long productId,
        String productName,
        String lotCode,
        String movementType,
        BigDecimal quantityBase,
        BigDecimal balanceAfter,
        String referenceType,
        Long referenceId,
        Long userId,
        Instant occurredAt,
        String notes
) {
    public static MovementDTO from(InventoryMovement movement) {
        return new MovementDTO(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getLot() != null ? movement.getLot().getLotCode() : null,
                movement.getMovementType().name(),
                movement.getQuantityBase(),
                movement.getBalanceAfter(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getUserId(),
                movement.getOccurredAt(),
                movement.getNotes()
        );
    }
}
