package com.boticario.modules.sales.infrastructure;

import com.boticario.modules.sales.domain.SalePayment;
import com.boticario.modules.sales.domain.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SalePaymentRepository extends JpaRepository<SalePayment, Long> {

    /** Cada fila: [0] = PaymentMethod, [1] = BigDecimal (suma de esa forma de pago). */
    @Query("SELECT p.paymentMethod, COALESCE(SUM(p.amount), 0) FROM SalePayment p " +
            "WHERE p.sale.status = :status AND p.sale.createdAt BETWEEN :from AND :to " +
            "GROUP BY p.paymentMethod")
    List<Object[]> sumAmountByPaymentMethodAndSaleStatusAndCreatedAtBetween(
            @Param("status") SaleStatus status, @Param("from") Instant from, @Param("to") Instant to);
}
