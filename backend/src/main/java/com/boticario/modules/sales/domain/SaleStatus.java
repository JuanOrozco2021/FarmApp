package com.boticario.modules.sales.domain;

/**
 * ACTIVA: la venta ya descargo inventario (via FEFO) y tiene sus pagos
 * registrados. ANULADA: se revirtio por completo — cada lote consumido se
 * repone exactamente (increaseStockToLot) y queda un movimiento
 * DEVOLUCION_VENTA nuevo por cada uno; la venta original nunca se borra
 * ni se reescribe (regla 13 del encargo: kardex inmutable).
 */
public enum SaleStatus {
    ACTIVA,
    ANULADA
}
