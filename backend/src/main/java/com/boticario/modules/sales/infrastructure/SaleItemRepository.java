package com.boticario.modules.sales.infrastructure;

import com.boticario.modules.sales.domain.SaleItem;
import com.boticario.modules.sales.domain.SaleStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    /** Cada fila: [0] = productId, [1] = productName, [2] = cantidad base vendida, [3] = ingresos (lineTotal). */
    @Query("SELECT i.presentation.product.id, i.presentation.product.name, " +
            "COALESCE(SUM(i.quantityBase), 0), COALESCE(SUM(i.lineTotal), 0) " +
            "FROM SaleItem i WHERE i.sale.status = :status AND i.sale.createdAt BETWEEN :from AND :to " +
            "GROUP BY i.presentation.product.id, i.presentation.product.name " +
            "ORDER BY SUM(i.lineTotal) DESC")
    List<Object[]> topProductsByRevenue(
            @Param("status") SaleStatus status, @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
