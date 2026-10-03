package com.boticario.modules.inventory.api;

import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.inventory.api.dto.AdjustmentDTO;
import com.boticario.modules.inventory.api.dto.CreateAdjustmentRequest;
import com.boticario.modules.inventory.api.dto.InventoryValuationDTO;
import com.boticario.modules.inventory.api.dto.LotDTO;
import com.boticario.modules.inventory.api.dto.MovementDTO;
import com.boticario.modules.inventory.api.dto.StockDTO;
import com.boticario.modules.inventory.application.InventoryAdjustmentService;
import com.boticario.modules.inventory.application.InventoryQueryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consultas de inventario (stock, lotes, kardex, alertas) y el endpoint de
 * ajustes manuales. Las consultas son de solo lectura y estan disponibles
 * para cualquier usuario autenticado; crear un ajuste requiere el permiso
 * INVENTORY_ADJUST (tipicamente ADMIN/FARMACEUTICO).
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryQueryService queryService;
    private final InventoryAdjustmentService adjustmentService;

    public InventoryController(InventoryQueryService queryService, InventoryAdjustmentService adjustmentService) {
        this.queryService = queryService;
        this.adjustmentService = adjustmentService;
    }

    @GetMapping("/products/{productId}/stock")
    public StockDTO getStock(@PathVariable Long productId) {
        return queryService.getStock(productId);
    }

    @GetMapping("/products/{productId}/lots")
    public List<LotDTO> getLots(@PathVariable Long productId) {
        return queryService.getLots(productId);
    }

    @GetMapping("/products/{productId}/kardex")
    public PageResponse<MovementDTO> getKardex(@PathVariable Long productId, Pageable pageable) {
        return queryService.getKardex(productId, pageable);
    }

    @GetMapping("/valuation")
    public InventoryValuationDTO getValuation() {
        return queryService.getValuation();
    }

    @GetMapping("/alerts/low-stock")
    public List<StockDTO> lowStockAlerts() {
        return queryService.lowStockAlerts();
    }

    @GetMapping("/alerts/expired-lots")
    public List<LotDTO> expiredLots() {
        return queryService.expiredLots();
    }

    @GetMapping("/alerts/expiring-lots")
    public List<LotDTO> expiringSoonLots() {
        return queryService.expiringSoonLots();
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAuthority('INVENTORY_ADJUST')")
    @ResponseStatus(HttpStatus.CREATED)
    public AdjustmentDTO createAdjustment(@Valid @RequestBody CreateAdjustmentRequest request) {
        return adjustmentService.create(request);
    }
}
