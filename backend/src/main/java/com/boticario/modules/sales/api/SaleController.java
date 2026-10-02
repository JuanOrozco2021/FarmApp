package com.boticario.modules.sales.api;

import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.sales.api.dto.CancelSaleRequest;
import com.boticario.modules.sales.api.dto.CreateSaleRequest;
import com.boticario.modules.sales.api.dto.SaleDTO;
import com.boticario.modules.sales.api.dto.SaleSummaryDTO;
import com.boticario.modules.sales.application.SaleService;
import com.boticario.modules.sales.domain.SaleStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Registrar una venta requiere SALE_REGISTER (lo tienen CAJERO,
 * FARMACEUTICO y ADMIN); el descuento por item se valida DENTRO de
 * SaleService segun SALE_APPLY_DISCOUNT, no aqui, porque es una regla por
 * item y no por endpoint. Anular requiere SALE_CANCEL (no lo tiene
 * CAJERO). Las consultas quedan abiertas a cualquier usuario autenticado.
 */
@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public PageResponse<SaleSummaryDTO> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) Long customerId,
            Pageable pageable) {
        return saleService.search(search, status, customerId, pageable);
    }

    @GetMapping("/{id}")
    public SaleDTO findById(@PathVariable Long id) {
        return saleService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SALE_REGISTER')")
    @ResponseStatus(HttpStatus.CREATED)
    public SaleDTO create(@Valid @RequestBody CreateSaleRequest request) {
        return saleService.create(request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SALE_CANCEL')")
    public SaleDTO cancel(@PathVariable Long id, @Valid @RequestBody CancelSaleRequest request) {
        return saleService.cancel(id, request);
    }
}
