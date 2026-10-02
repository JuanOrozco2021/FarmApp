package com.boticario.modules.identity.application;

import com.boticario.modules.identity.api.dto.LoginRequest;
import com.boticario.modules.identity.api.dto.LoginResponse;
import com.boticario.modules.identity.domain.User;
import com.boticario.modules.identity.infrastructure.UserRepository;
import com.boticario.security.jwt.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager,
                        JwtTokenProvider tokenProvider,
                        UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String accessToken = tokenProvider.generateAccessToken(principal);

        // El usuario ya quedo validado por el AuthenticationManager; se recarga
        // para devolver los datos de presentacion (nombre, roles legibles).
        User user = userRepository.findByUsername(request.username()).orElseThrow();

        Set<String> roleCodes = user.getRoles().stream().map(r -> r.getCode()).collect(Collectors.toSet());
        Set<String> permissionCodes = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getCode())
                .collect(Collectors.toSet());

        return new LoginResponse(accessToken, "Bearer", user.getUsername(), user.getFullName(), roleCodes, permissionCodes);
    }
}
