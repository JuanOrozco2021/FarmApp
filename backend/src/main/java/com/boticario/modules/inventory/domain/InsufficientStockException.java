package com.boticario.modules.inventory.domain;

import com.boticario.common.exception.BusinessRuleException;

import java.math.BigDecimal;

/**
 * Se lanza cuando se intenta descontar mas de lo disponible. Nunca se
 * permite continuar la transaccion cuando esto ocurre (regla 10 del
 * encargo: nunca stock negativo, sin excepcion).
 */
public class InsufficientStockException extends BusinessRuleException {
    public InsufficientStockException(Long productId, BigDecimal requested, BigDecimal available) {
        super("Stock insuficiente para el producto " + productId
                + " (solicitado: " + requested + ", disponible: " + available + ")");
    }
}
