package com.boticario.modules.identity.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateRolesRequest(
        @NotEmpty(message = "debe asignar al menos un rol") Set<String> roleCodes
) {
}
