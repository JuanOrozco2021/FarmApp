package com.boticario.modules.identity.application;

import com.boticario.modules.identity.domain.Role;
import com.boticario.modules.identity.domain.User;
import com.boticario.modules.identity.infrastructure.RoleRepository;
import com.boticario.modules.identity.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Crea el usuario administrador inicial en el primer arranque, si todavia
 * no existe ningun usuario. Se hace en codigo (no en un migration SQL) para
 * usar el mismo PasswordEncoder que el resto de la aplicacion y no dejar
 * un hash de contrasena hardcodeado en el repositorio.
 *
 * Credenciales por defecto SOLO para desarrollo local (documentadas en el
 * README): usuario "admin" / contrasena "Admin123!". Se pueden sobreescribir
 * con las variables de entorno SEED_ADMIN_USERNAME, SEED_ADMIN_PASSWORD y
 * SEED_ADMIN_EMAIL — se recomienda cambiarlas antes de desplegar a un
 * ambiente real.
 */
@Component
public class AdminUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminUsername;
    private final String adminPassword;
    private final String adminEmail;

    public AdminUserSeeder(UserRepository userRepository,
                            RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.seed.admin-username}") String adminUsername,
                            @Value("${app.seed.admin-password}") String adminPassword,
                            @Value("${app.seed.admin-email}") String adminEmail) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminEmail = adminEmail;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return; // ya hay usuarios: no se toca nada
        }

        Role adminRole = roleRepository.findByCode("ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "El rol ADMIN no existe. Verifica que la migracion V2__seed_identity_data.sql se haya aplicado."));

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setFullName("Administrador");
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setActive(true);
        admin.setRoles(Set.of(adminRole));

        userRepository.save(admin);

        log.warn("Usuario administrador inicial creado: username='{}'. "
                + "Cambia la contrasena por defecto antes de usar este sistema fuera de desarrollo local.",
                adminUsername);
    }
}
