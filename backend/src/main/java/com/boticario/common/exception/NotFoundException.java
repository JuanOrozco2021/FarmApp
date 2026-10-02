package com.boticario.common.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String entity, Object id) {
        super(entity + " no encontrado(a) con id " + id);
    }
}
