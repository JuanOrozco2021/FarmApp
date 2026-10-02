package com.boticario.modules.purchasing.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank String name,
        String taxId,
        String phone,
        @Email(message = "el correo no tiene un formato valido") String email,
        String address,
        String contactName,
        @Min(value = 0, message = "los dias de credito no pueden ser negativos") Integer paymentTermsDays,
        String notes
) {
}
