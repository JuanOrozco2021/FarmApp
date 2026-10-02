package com.boticario.modules.customers.api.dto;

import com.boticario.modules.customers.domain.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        DocumentType documentType,
        String documentNumber,
        @NotBlank String name,
        String phone,
        @Email(message = "el correo no tiene un formato valido") String email,
        String address
) {
}
