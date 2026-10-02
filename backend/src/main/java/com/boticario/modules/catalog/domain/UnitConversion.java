package com.boticario.modules.catalog.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Unico punto del sistema que convierte entre la unidad de una presentacion
 * y la unidad base del producto. La usan catalogo (validaciones), compras,
 * ventas e inyectologia — nunca se reimplementa en otro lugar, y JAMAS se
 * calcula en el frontend (regla central del encargo).
 *
 * Ejemplo: Ibuprofeno con unidad base PASTILLA y presentacion TABLETA de
 * factor 10 -> vender "1 tableta" descuenta 10 PASTILLA del inventario.
 */
public final class UnitConversion {

    private UnitConversion() {
    }

    /** Cuantas unidades base representa una cantidad dada en una presentacion. */
    public static BigDecimal toBaseUnits(ProductPresentation presentation, BigDecimal quantityInPresentation) {
        requirePositive(quantityInPresentation);
        return quantityInPresentation.multiply(presentation.getConversionFactor());
    }

    /** Inverso: cuantas unidades de la presentacion representa una cantidad en unidad base. */
    public static BigDecimal fromBaseUnits(ProductPresentation presentation, BigDecimal quantityInBase) {
        requirePositive(quantityInBase);
        return quantityInBase.divide(presentation.getConversionFactor(), 6, RoundingMode.HALF_UP);
    }

    private static void requirePositive(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new InvalidQuantityException(quantity);
        }
    }
}
