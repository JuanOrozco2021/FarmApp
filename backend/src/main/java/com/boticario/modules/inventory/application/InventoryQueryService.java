package com.boticario.modules.inventory.application;

import com.boticario.common.exception.NotFoundException;
import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.catalog.domain.Product;
import com.boticario.modules.catalog.infrastructure.ProductRepository;
import com.boticario.modules.inventory.api.dto.InventoryValuationDTO;
import com.boticario.modules.inventory.api.dto.LotDTO;
import com.boticario.modules.inventory.api.dto.MovementDTO;
import com.boticario.modules.inventory.api.dto.StockDTO;
import com.boticario.modules.inventory.domain.InventoryLot;
import com.boticario.modules.inventory.domain.InventoryStock;
import com.boticario.modules.inventory.infrastructure.InventoryLotRepository;
import com.boticario.modules.inventory.infrastructure.InventoryMovementRepository;
import com.boticario.modules.inventory.infrastructure.InventoryStockRepository;
import com.boticario.modules.settings.infrastructure.BusinessSettingsRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Consultas de solo lectura de inventario: stock, lotes, kardex y alertas. */
@Service
public class InventoryQueryService {

    private final InventoryStockRepository stockRepository;
    private final InventoryLotRepository lotRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final BusinessSettingsRepository settingsRepository;

    public InventoryQueryService(InventoryStockRepository stockRepository,
                                  InventoryLotRepository lotRepository,
                                  InventoryMovementRepository movementRepository,
                                  ProductRepository productRepository,
                                  BusinessSettingsRepository settingsRepository) {
        this.stockRepository = stockRepository;
        this.lotRepository = lotRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.settingsRepository = settingsRepository;
    }

    @Transactional(readOnly = true)
    public StockDTO getStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        BigDecimal quantity = stockRepository.findById(productId)
                .map(InventoryStock::getQuantityBase)
                .orElse(BigDecimal.ZERO);

        boolean lowStock = quantity.compareTo(product.getMinStockBase()) <= 0;

        return new StockDTO(product.getId(), product.getInternalCode(), product.getName(),
                product.getBaseUnit().getCode(), quantity, product.getMinStockBase(), product.getMaxStockBase(), lowStock);
    }

    @Transactional(readOnly = true)
    public List<LotDTO> getLots(Long productId) {
        LocalDate today = LocalDate.now();
        LocalDate alertCutoff = today.plusDays(alertDays());

        return lotRepository.findByProductIdAndActiveTrueOrderByExpirationDateAsc(productId).stream()
                .map(lot -> LotDTO.from(lot, today, alertCutoff))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementDTO> getKardex(Long productId, Pageable pageable) {
        var page = movementRepository.findByProductIdOrderByOccurredAtDesc(productId, pageable).map(MovementDTO::from);
        return PageResponse.from(page);
    }

    /** Productos activos cuyo stock actual ya llego (o esta por debajo) de su stock minimo. */
    @Transactional(readOnly = true)
    public List<StockDTO> lowStockAlerts() {
        return stockRepository.findLowStock().stream()
                .map(stock -> {
                    Product product = stock.getProduct();
                    return new StockDTO(product.getId(), product.getInternalCode(), product.getName(),
                            product.getBaseUnit().getCode(), stock.getQuantityBase(),
                            product.getMinStockBase(), product.getMaxStockBase(), true);
                })
                .toList();
    }

    /** Lotes ya vencidos (fecha <= hoy). */
    @Transactional(readOnly = true)
    public List<LotDTO> expiredLots() {
        LocalDate today = LocalDate.now();
        return lotRepository.findExpiringOrExpired(today).stream()
                .map(lot -> LotDTO.from(lot, today, today))
                .toList();
    }

    /** Lotes que vencen dentro de la ventana configurada (business_settings.expiration_alert_days), sin incluir los ya vencidos. */
    @Transactional(readOnly = true)
    public List<LotDTO> expiringSoonLots() {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(alertDays());

        return lotRepository.findExpiringOrExpired(cutoff).stream()
                .filter(lot -> lot.getExpirationDate().isAfter(today))
                .map(lot -> LotDTO.from(lot, today, cutoff))
                .toList();
    }

    /**
     * Valor total de lo que hay fisicamente en inventario en este momento,
     * calculado al vuelo (no persiste nada, no requiere migracion de datos
     * existentes). Dos totales porque "valor de inventario" admite dos
     * lecturas validas: a costo (lo que costo comprarlo -- el sentido
     * contable, pero depende de que purchase_price este cargado) y a
     * precio de venta (lo que se recibiria si se vendiera todo, siempre
     * completo porque sale_price es obligatorio).
     */
    @Transactional(readOnly = true)
    public InventoryValuationDTO getValuation() {
        List<Object[]> rows = stockRepository.findValuationRows();

        BigDecimal valueAtCost = BigDecimal.ZERO;
        BigDecimal valueAtSalePrice = BigDecimal.ZERO;
        int withoutCost = 0;

        for (Object[] row : rows) {
            BigDecimal quantityBase = (BigDecimal) row[0];
            BigDecimal purchasePrice = (BigDecimal) row[1];
            BigDecimal salePrice = (BigDecimal) row[2];

            valueAtSalePrice = valueAtSalePrice.add(quantityBase.multiply(salePrice));

            if (purchasePrice != null) {
                valueAtCost = valueAtCost.add(quantityBase.multiply(purchasePrice));
            } else {
                withoutCost++;
            }
        }

        return new InventoryValuationDTO(valueAtCost, valueAtSalePrice, withoutCost, rows.size());
    }

    private int alertDays() {
        return settingsRepository.findById((short) 1)
                .map(s -> s.getExpirationAlertDays())
                .orElse(30);
    }
}
