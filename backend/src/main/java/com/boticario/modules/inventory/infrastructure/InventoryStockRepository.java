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

    /**
     * Una fila por producto con stock fisico (> 0): su cantidad en unidad
     * base, y el costo/precio de venta de su presentacion base (la unica
     * presentacion cuyo precio ya esta expresado por unidad base, sin
     * necesidad de dividir por ningun factor de conversion). purchase_price
     * puede venir null -- se maneja en el service, no aqui.
     */
    @Query("""
            select s.quantityBase, pp.purchasePrice, pp.salePrice
            from InventoryStock s
            join ProductPresentation pp on pp.product.id = s.productId
            where pp.basePresentation = true and pp.active = true and s.quantityBase > 0
            """)
    List<Object[]> findValuationRows();
}
