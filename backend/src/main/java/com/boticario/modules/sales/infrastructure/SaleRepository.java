package com.boticario.modules.sales.infrastructure;

import com.boticario.modules.sales.domain.Sale;
import com.boticario.modules.sales.domain.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    @Query(value = "SELECT nextval('sale_code_seq')", nativeQuery = true)
    Long nextSaleCodeSequence();

    long countByStatusAndCreatedAtBetween(SaleStatus status, Instant from, Instant to);

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.status = :status AND s.createdAt BETWEEN :from AND :to")
    BigDecimal sumTotalByStatusAndCreatedAtBetween(@Param("status") SaleStatus status, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COALESCE(SUM(s.discountTotal), 0) FROM Sale s WHERE s.status = :status AND s.createdAt BETWEEN :from AND :to")
    BigDecimal sumDiscountByStatusAndCreatedAtBetween(@Param("status") SaleStatus status, @Param("from") Instant from, @Param("to") Instant to);
}
