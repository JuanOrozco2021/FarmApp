package com.boticario.modules.inventory.domain;

/** Subconjunto de MovementType que un usuario puede disparar manualmente desde un ajuste. */
public enum AdjustmentType {
    POSITIVO(MovementType.AJUSTE_POSITIVO),
    NEGATIVO(MovementType.AJUSTE_NEGATIVO),
    MERMA(MovementType.MERMA),
    VENCIMIENTO(MovementType.VENCIMIENTO);

    private final MovementType movementType;

    AdjustmentType(MovementType movementType) {
        this.movementType = movementType;
    }

    public MovementType toMovementType() {
        return movementType;
    }

    public boolean isPositive() {
        return this == POSITIVO;
    }
}
