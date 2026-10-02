package com.boticario.modules.reports.api;

import com.boticario.modules.reports.api.dto.InventoryAlertsSummaryDTO;
import com.boticario.modules.reports.api.dto.SalesSummaryDTO;
import com.boticario.modules.reports.api.dto.TopProductDTO;
import com.boticario.modules.reports.application.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Reportes de solo lectura para el Dashboard (Fase 9). Igual criterio que
 * las consultas de Ventas/Compras/Clientes: abierto a cualquier usuario
 * autenticado, sin permiso adicional — no expone nada que esas pantallas
 * no muestren ya por separado.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales-summary")
    public SalesSummaryDTO salesSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return reportService.salesSummary(from, to);
    }

    @GetMapping("/top-products")
    public List<TopProductDTO> topProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "10") int limit) {
        return reportService.topProducts(from, to, limit);
    }

    @GetMapping("/inventory-alerts-summary")
    public InventoryAlertsSummaryDTO inventoryAlertsSummary() {
        return reportService.inventoryAlertsSummary();
    }
}
