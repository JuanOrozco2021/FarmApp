package com.boticario.common.exception;

/** Se lanza cuando una operacion viola una regla de negocio (no un error tecnico). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
