package com.boticario.common.exception;

/** Ej: nombre de usuario, email o codigo de barras ya existente. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
