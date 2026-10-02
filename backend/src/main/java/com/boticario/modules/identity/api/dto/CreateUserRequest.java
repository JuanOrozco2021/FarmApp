package com.boticario.modules.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 60) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "la contrasena debe tener al menos 8 caracteres")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password,
        @NotBlank String fullName,
        @NotEmpty(message = "debe asignar al menos un rol") Set<String> roleCodes
) {
}
