package com.boticario.modules.identity.api.dto;

import java.util.Set;

public record LoginResponse(
        String accessToken,
        String tokenType,
        String username,
        String fullName,
        Set<String> roles,
        Set<String> permissions
) {
}
