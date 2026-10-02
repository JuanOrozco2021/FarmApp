package com.boticario.modules.inventory.infrastructure;

import com.boticario.modules.inventory.domain.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long>, JpaSpecificationExecutor<InventoryMovement> {
    Page<InventoryMovement> findByProductIdOrderByOccurredAtDesc(Long productId, Pageable pageable);
}
