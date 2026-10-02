package com.boticario.modules.identity.api;

import com.boticario.modules.identity.api.dto.LoginRequest;
import com.boticario.modules.identity.api.dto.LoginResponse;
import com.boticario.modules.identity.application.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Los tokens son stateless (JWT) y de corta duracion: "logout" es
     * responsabilidad del cliente (descartar el token). Se deja el endpoint
     * por consistencia de API y como punto de extension para una futura
     * lista de revocacion si el negocio lo requiere.
     */
    @PostMapping("/logout")
    public void logout() {
        SecurityContextHolder.clearContext();
    }
}
