package com.boticario.modules.inventory.infrastructure;

import com.boticario.modules.inventory.domain.InventoryStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {

    /**
     * SELECT ... FOR UPDATE real: bloquea la fila de stock del producto
     * hasta que la transaccion confirme o revierta. Es el mecanismo
     * central de la estrategia de concurrencia (regla 24 del encargo) —
     * dos ventas simultaneas del mismo producto se serializan aqui.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from InventoryStock s where s.productId = :productId")
    Optional<InventoryStock> findByProductIdForUpdate(@Param("productId") Long productId);

    @Query("select s from InventoryStock s where s.quantityBase <= s.product.minStockBase and s.product.status = 'ACTIVE'")
    List<InventoryStock> findLowStock();
}
