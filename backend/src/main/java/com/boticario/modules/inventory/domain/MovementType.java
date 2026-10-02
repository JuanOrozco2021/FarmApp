package com.boticario.modules.inventory.domain;

/** Tipos de movimiento del kardex (regla 13 del encargo). */
public enum MovementType {
    COMPRA,
    VENTA,
    DEVOLUCION_COMPRA,
    DEVOLUCION_VENTA,
    AJUSTE_POSITIVO,
    AJUSTE_NEGATIVO,
    MERMA,
    VENCIMIENTO,
    TRASLADO
}
