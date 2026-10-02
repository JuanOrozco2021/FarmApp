package com.boticario.modules.identity.infrastructure;

import com.boticario.modules.identity.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
}
