package com.boticario.modules.catalog.infrastructure;

import com.boticario.modules.catalog.domain.ActiveIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveIngredientRepository extends JpaRepository<ActiveIngredient, Long> {
    boolean existsByNameIgnoreCase(String name);
}
