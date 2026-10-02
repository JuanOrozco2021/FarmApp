package com.boticario.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private final JwtTokenProvider tokenProvider = new JwtTokenProvider(
            "unit-test-secret-key-must-be-at-least-32-bytes-long", 30);

    @Test
    void generatesAndValidatesATokenWithEmbeddedAuthorities() {
        var userDetails = new User("cajero1", "irrelevant",
                List.of(new SimpleGrantedAuthority("ROLE_CAJERO"), new SimpleGrantedAuthority("SALE_REGISTER")));

        String token = tokenProvider.generateAccessToken(userDetails);

        assertThat(tokenProvider.isValid(token)).isTrue();
        assertThat(tokenProvider.getUsername(token)).isEqualTo("cajero1");
        assertThat(tokenProvider.getAuthorities(token))
                .containsExactlyInAnyOrder("ROLE_CAJERO", "SALE_REGISTER");
    }

    @Test
    void rejectsATamperedToken() {
        var userDetails = new User("cajero1", "irrelevant", List.of());
        String token = tokenProvider.generateAccessToken(userDetails);

        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(tokenProvider.isValid(tampered)).isFalse();
    }
}
