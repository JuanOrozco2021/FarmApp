package com.boticario.modules.identity.api;

import com.boticario.modules.identity.api.dto.ChangeOwnPasswordRequest;
import com.boticario.modules.identity.api.dto.UserSummaryDTO;
import com.boticario.modules.identity.application.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autoservicio sobre la cuenta del usuario ya autenticado. Distinto de
 * UserController (que administra a OTROS usuarios y exige USER_MANAGE):
 * aqui cualquier usuario autenticado puede cambiar su propia contrasena.
 * No vive bajo /api/auth/** a proposito: esa ruta esta en PUBLIC_ENDPOINTS
 * (login/logout no requieren token) y anadir aqui una ruta protegida bajo
 * ese prefijo la dejaria sin autenticacion por el permitAll de SecurityConfig.
 * Al quedar fuera de /api/auth/**, la regla global "anyRequest().authenticated()"
 * la protege normalmente.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/password")
    public UserSummaryDTO changePassword(Authentication authentication, @Valid @RequestBody ChangeOwnPasswordRequest request) {
        return userService.changeOwnPassword(authentication.getName(), request);
    }
}
