package com.boticario.modules.inventory.application;

import java.math.BigDecimal;

/**
 * Una porcion de una operacion de salida que se tomo de un lote concreto.
 * Una sola venta puede generar varias asignaciones si tuvo que tomar de
 * mas de un lote (ver caso 5 del encargo: FEFO puede repartir entre lotes).
 * Sales/inyectologia (Fases 6/8) usaran esto para su propia trazabilidad
 * (ej. sale_item_lots) sin tener que reimplementar la seleccion FEFO.
 */
public record LotAllocation(Long lotId, String lotCode, BigDecimal quantityBase, Long movementId) {
}
