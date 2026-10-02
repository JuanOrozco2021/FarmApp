package com.boticario.modules.catalog.api.dto;

import java.math.BigDecimal;

/**
 * Lo que el POS (Fase 6) y el alta de producto necesitan al escanear un
 * codigo: a que producto y presentacion pertenece, el factor para convertir
 * a unidad base, y el precio vigente. Esta resolucion ocurre siempre en
 * backend (regla 27 del encargo).
 */
public record BarcodeResolutionDTO(
        String barcode,
        Long productId,
        String productName,
        Long presentationId,
        String unitCode,
        BigDecimal conversionFactor,
        BigDecimal salePrice,
        boolean requiresPrescription,
        boolean fractionable
) {
}
