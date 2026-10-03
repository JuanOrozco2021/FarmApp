package com.boticario.modules.inventory.api.dto;

import java.math.BigDecimal;

/**
 * Valor total de lo que hay fisicamente en inventario, calculado al vuelo
 * a partir de inventory_stock y el precio de la presentacion base de cada
 * producto (nunca se persiste, nunca requiere migracion).
 *
 * valueAtCost puede quedar subestimado si algunos productos no tienen
 * purchase_price cargado (campo opcional, se llena a mano en el catalogo)
 * -- productsWithoutCost le dice al frontend cuantos para que lo advierta
 * en vez de mostrar un total silenciosamente incompleto. valueAtSalePrice
 * siempre esta completo porque sale_price es obligatorio.
 */
public record InventoryValuationDTO(
        BigDecimal valueAtCost,
        BigDecimal valueAtSalePrice,
        int productsWithoutCost,
        int productsWithStock
) {
}
