package com.boticario.modules.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * WRITE_ONLY en ambas contrasenas: se deserializan normalmente desde el
 * request, pero Jackson nunca las vuelve a serializar -- ni en una
 * respuesta HTTP ni, mas importante, en el before_json que AuditAspect
 * guarda en audit_logs (serializa los argumentos del metodo tal cual).
 * Sin esto, la contrasena quedaria en texto plano en la auditoria.
 */
public record ChangeOwnPasswordRequest(
        @NotBlank
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String currentPassword,

        @NotBlank @Size(min = 8, message = "la contrasena debe tener al menos 8 caracteres")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String newPassword
) {
}
