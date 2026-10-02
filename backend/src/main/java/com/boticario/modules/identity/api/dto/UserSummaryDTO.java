package com.boticario.modules.identity.api.dto;

import com.boticario.modules.identity.domain.User;

import java.util.Set;
import java.util.stream.Collectors;

public record UserSummaryDTO(
        Long id,
        String username,
        String email,
        String fullName,
        boolean active,
        Set<String> roles
) {
    public static UserSummaryDTO from(User user) {
        return new UserSummaryDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.isActive(),
                user.getRoles().stream().map(r -> r.getCode()).collect(Collectors.toSet())
        );
    }
}
