package com.boticario.modules.identity.api;

import com.boticario.modules.identity.api.dto.CreateUserRequest;
import com.boticario.modules.identity.api.dto.ResetPasswordRequest;
import com.boticario.modules.identity.api.dto.UpdateRolesRequest;
import com.boticario.modules.identity.api.dto.UserSummaryDTO;
import com.boticario.modules.identity.application.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserSummaryDTO> findAll() {
        return userService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummaryDTO create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PatchMapping("/{id}/active")
    public UserSummaryDTO setActive(@PathVariable Long id, @RequestParam boolean active) {
        return userService.setActive(id, active);
    }

    @PutMapping("/{id}/roles")
    public UserSummaryDTO updateRoles(@PathVariable Long id, @Valid @RequestBody UpdateRolesRequest request) {
        return userService.updateRoles(id, request.roleCodes());
    }

    @PatchMapping("/{id}/password")
    public UserSummaryDTO resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(id, request);
    }
}
