package com.boticario.modules.catalog.infrastructure;

import com.boticario.modules.catalog.domain.Laboratory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaboratoryRepository extends JpaRepository<Laboratory, Long> {
    boolean existsByNameIgnoreCase(String name);
}
