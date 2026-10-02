package com.boticario.modules.inventory.infrastructure;

import com.boticario.modules.inventory.domain.InventoryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
}
