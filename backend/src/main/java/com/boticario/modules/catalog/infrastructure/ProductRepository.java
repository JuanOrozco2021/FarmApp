package com.boticario.modules.catalog.infrastructure;

import com.boticario.modules.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsByInternalCode(String internalCode);

    @org.springframework.data.jpa.repository.Query(value = "SELECT nextval('product_internal_code_seq')", nativeQuery = true)
    Long nextInternalCodeSequence();
}
