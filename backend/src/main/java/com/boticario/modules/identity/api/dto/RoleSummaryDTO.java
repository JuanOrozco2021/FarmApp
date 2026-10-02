package com.boticario.modules.identity.api.dto;

import com.boticario.modules.identity.domain.Role;

import java.util.Set;
import java.util.stream.Collectors;

public record RoleSummaryDTO(Long id, String code, String name, Set<String> permissions) {
    public static RoleSummaryDTO from(Role role) {
        return new RoleSummaryDTO(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getPermissions().stream().map(p -> p.getCode()).collect(Collectors.toSet())
        );
    }
}
