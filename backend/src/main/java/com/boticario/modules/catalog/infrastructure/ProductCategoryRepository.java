package com.boticario.modules.catalog.infrastructure;

import com.boticario.modules.catalog.domain.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    boolean existsByNameIgnoreCase(String name);
}
