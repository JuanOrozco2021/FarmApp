package com.boticario.modules.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** WRITE_ONLY: ver ChangeOwnPasswordRequest -- evita que quede en texto plano en audit_logs. */
public record ResetPasswordRequest(
        @NotBlank @Size(min = 8, message = "la contrasena debe tener al menos 8 caracteres")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String newPassword
) {
}
