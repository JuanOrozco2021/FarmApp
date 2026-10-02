package com.boticario.modules.reports.application;

import com.boticario.common.exception.BusinessRuleException;
import com.boticario.modules.inventory.application.InventoryQueryService;
import com.boticario.modules.reports.api.dto.InventoryAlertsSummaryDTO;
import com.boticario.modules.reports.api.dto.PaymentBreakdownDTO;
import com.boticario.modules.reports.api.dto.SalesSummaryDTO;
import com.boticario.modules.reports.api.dto.TopProductDTO;
import com.boticario.modules.sales.domain.PaymentMethod;
import com.boticario.modules.sales.domain.SaleStatus;
import com.boticario.modules.sales.infrastructure.SaleItemRepository;
import com.boticario.modules.sales.infrastructure.SalePaymentRepository;
import com.boticario.modules.sales.infrastructure.SaleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Reportes de solo lectura para el Dashboard (Fase 9). No introduce
 * ningun dato nuevo: agrega lo que ya registran Ventas e Inventario. Las
 * alertas de inventario reutilizan InventoryQueryService (misma logica
 * que las pantallas propias de Inventario en la Fase 4) en vez de
 * duplicarla.
 */
@Service
public class ReportService {

    private final SaleRepository saleRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final SaleItemRepository saleItemRepository;
    private final InventoryQueryService inventoryQueryService;

    public ReportService(SaleRepository saleRepository,
                          SalePaymentRepository salePaymentRepository,
                          SaleItemRepository saleItemRepository,
                          InventoryQueryService inventoryQueryService) {
        this.saleRepository = saleRepository;
        this.salePaymentRepository = salePaymentRepository;
        this.saleItemRepository = saleItemRepository;
        this.inventoryQueryService = inventoryQueryService;
    }

    @Transactional(readOnly = true)
    public SalesSummaryDTO salesSummary(Instant from, Instant to) {
        validateRange(from, to);

        long totalSales = saleRepository.countByStatusAndCreatedAtBetween(SaleStatus.ACTIVA, from, to);
        BigDecimal totalRevenue = saleRepository.sumTotalByStatusAndCreatedAtBetween(SaleStatus.ACTIVA, from, to);
        BigDecimal totalDiscount = saleRepository.sumDiscountByStatusAndCreatedAtBetween(SaleStatus.ACTIVA, from, to);
        BigDecimal averageTicket = totalSales > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalSales), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<PaymentBreakdownDTO> paymentBreakdown = salePaymentRepository
                .sumAmountByPaymentMethodAndSaleStatusAndCreatedAtBetween(SaleStatus.ACTIVA, from, to)
                .stream()
                .map(row -> new PaymentBreakdownDTO(((PaymentMethod) row[0]).name(), (BigDecimal) row[1]))
                .toList();

        return new SalesSummaryDTO(totalSales, totalRevenue, totalDiscount, averageTicket, paymentBreakdown);
    }

    @Transactional(readOnly = true)
    public List<TopProductDTO> topProducts(Instant from, Instant to, int limit) {
        validateRange(from, to);
        if (limit <= 0) {
            throw new BusinessRuleException("El limite de productos debe ser mayor que cero");
        }

        return saleItemRepository.topProductsByRevenue(SaleStatus.ACTIVA, from, to, PageRequest.of(0, limit))
                .stream()
                .map(row -> new TopProductDTO((Long) row[0], (String) row[1], (BigDecimal) row[2], (BigDecimal) row[3]))
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryAlertsSummaryDTO inventoryAlertsSummary() {
        int lowStock = inventoryQueryService.lowStockAlerts().size();
        int expiringSoon = inventoryQueryService.expiringSoonLots().size();
        int expired = inventoryQueryService.expiredLots().size();
        return new InventoryAlertsSummaryDTO(lowStock, expiringSoon, expired);
    }

    private void validateRange(Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("La fecha inicial no puede ser posterior a la fecha final");
        }
    }
}
