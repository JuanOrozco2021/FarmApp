package com.boticario.modules.catalog.domain;

import com.boticario.common.exception.BusinessRuleException;

import java.math.BigDecimal;

public class InvalidQuantityException extends BusinessRuleException {
    public InvalidQuantityException(BigDecimal quantity) {
        super("La cantidad debe ser mayor que cero (recibido: " + quantity + ")");
    }
}
