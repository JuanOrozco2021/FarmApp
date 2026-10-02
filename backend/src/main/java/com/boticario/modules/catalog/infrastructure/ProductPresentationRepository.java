package com.boticario.modules.catalog.infrastructure;

import com.boticario.modules.catalog.domain.ProductPresentation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductPresentationRepository extends JpaRepository<ProductPresentation, Long> {
    List<ProductPresentation> findByProductIdAndActiveTrue(Long productId);
}
