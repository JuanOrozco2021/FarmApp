package com.boticario.modules.inventory.infrastructure;

import com.boticario.modules.inventory.domain.InventoryLot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InventoryLotRepository extends JpaRepository<InventoryLot, Long> {

    Optional<InventoryLot> findByProductIdAndLotCode(Long productId, String lotCode);

    List<InventoryLot> findByProductIdAndActiveTrueOrderByExpirationDateAsc(Long productId);

    /**
     * Bloquea los lotes activos del producto en orden FEFO (el que vence
     * primero, primero) para que el descuento sea atomico dentro de la
     * misma transaccion que ya tiene el lock de inventory_stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from InventoryLot l where l.product.id = :productId and l.active = true and l.quantityBase > 0 "
            + "order by l.expirationDate asc nulls last, l.id asc")
    List<InventoryLot> findActiveLotsForUpdateFefo(@Param("productId") Long productId);

    @Query("select l from InventoryLot l where l.active = true and l.expirationDate is not null "
            + "and l.expirationDate <= :cutoff and l.quantityBase > 0 order by l.expirationDate asc")
    List<InventoryLot> findExpiringOrExpired(@Param("cutoff") LocalDate cutoff);
}
