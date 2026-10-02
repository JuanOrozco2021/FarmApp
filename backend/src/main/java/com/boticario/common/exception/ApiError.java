package com.boticario.common.exception;

import java.time.Instant;
import java.util.List;

/** Formato unico de error para toda la API — nunca se exponen stack traces al cliente. */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<String> details
) {
    public static ApiError of(int status, String code, String message, List<String> details) {
        return new ApiError(Instant.now(), status, code, message, details);
    }
}
