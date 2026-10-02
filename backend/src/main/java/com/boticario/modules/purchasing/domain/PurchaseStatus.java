package com.boticario.modules.purchasing.domain;

/**
 * BORRADOR: editable, sin ningun efecto en inventario todavia.
 * RECIBIDA: inmutable en sus items; cada item ya genero su movimiento de
 * entrada (COMPRA) en el kardex via StockAllocationService.
 * ANULADA: solo alcanzable desde BORRADOR (una compra RECIBIDA no se anula
 * — para revertir stock ya ingresado se usa una devolucion a proveedor,
 * que queda registrada como tal en el kardex en vez de desaparecer).
 */
public enum PurchaseStatus {
    BORRADOR,
    RECIBIDA,
    ANULADA
}
