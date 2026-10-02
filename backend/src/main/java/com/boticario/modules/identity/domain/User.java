package com.boticario.modules.identity.domain;

import com.boticario.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /** Autoridades para Spring Security: ROLE_<codigo de rol> + codigo de cada permiso. */
    public Set<String> resolveAuthorities() {
        Set<String> authorities = roles.stream()
                .map(role -> "ROLE_" + role.getCode())
                .collect(Collectors.toCollection(HashSet::new));

        roles.forEach(role -> role.getPermissions()
                .forEach(permission -> authorities.add(permission.getCode())));

        return authorities;
    }
}
