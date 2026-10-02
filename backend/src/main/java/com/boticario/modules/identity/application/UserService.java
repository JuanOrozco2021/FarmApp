package com.boticario.modules.identity.application;

import com.boticario.common.audit.Audited;
import com.boticario.common.exception.BusinessRuleException;
import com.boticario.common.exception.DuplicateResourceException;
import com.boticario.common.exception.NotFoundException;
import com.boticario.modules.identity.api.dto.ChangeOwnPasswordRequest;
import com.boticario.modules.identity.api.dto.CreateUserRequest;
import com.boticario.modules.identity.api.dto.ResetPasswordRequest;
import com.boticario.modules.identity.api.dto.UserSummaryDTO;
import com.boticario.modules.identity.domain.Role;
import com.boticario.modules.identity.domain.User;
import com.boticario.modules.identity.infrastructure.RoleRepository;
import com.boticario.modules.identity.infrastructure.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Audited(action = "USER_CREATE", entity = "User")
    public UserSummaryDTO create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Ya existe un usuario con el nombre " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Ya existe un usuario con el correo " + request.email());
        }

        Set<Role> roles = request.roleCodes().stream()
                .map(code -> roleRepository.findByCode(code)
                        .orElseThrow(() -> new NotFoundException("Role", code)))
                .collect(Collectors.toSet());

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(roles);
        user.setActive(true);

        return UserSummaryDTO.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDTO> findAll() {
        return userRepository.findAll().stream().map(UserSummaryDTO::from).toList();
    }

    @Transactional
    @Audited(action = "USER_STATUS_CHANGE", entity = "User")
    public UserSummaryDTO setActive(Long id, boolean active) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
        user.setActive(active);
        return UserSummaryDTO.from(userRepository.save(user));
    }

    /** Reemplaza por completo el conjunto de roles asignados (no es un merge incremental). */
    @Transactional
    @Audited(action = "USER_ROLES_UPDATE", entity = "User")
    public UserSummaryDTO updateRoles(Long id, Set<String> roleCodes) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
        Set<Role> roles = roleCodes.stream()
                .map(code -> roleRepository.findByCode(code)
                        .orElseThrow(() -> new NotFoundException("Role", code)))
                .collect(Collectors.toSet());
        user.setRoles(roles);
        return UserSummaryDTO.from(userRepository.save(user));
    }

    /** Reseteo administrativo: no exige conocer la contrasena anterior (requiere USER_MANAGE en el controller). */
    @Transactional
    @Audited(action = "USER_PASSWORD_RESET", entity = "User")
    public UserSummaryDTO resetPassword(Long id, ResetPasswordRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        return UserSummaryDTO.from(userRepository.save(user));
    }

    /** Autoservicio: cualquier usuario autenticado cambia su propia contrasena, verificando la actual. */
    @Transactional
    @Audited(action = "USER_PASSWORD_CHANGE", entity = "User")
    public UserSummaryDTO changeOwnPassword(String username, ChangeOwnPasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User", username));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("La contrasena actual no es correcta");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        return UserSummaryDTO.from(userRepository.save(user));
    }
}
