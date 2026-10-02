package com.boticario.modules.purchasing.api;

import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.purchasing.api.dto.CreatePurchaseRequest;
import com.boticario.modules.purchasing.api.dto.PurchaseDTO;
import com.boticario.modules.purchasing.api.dto.PurchaseSummaryDTO;
import com.boticario.modules.purchasing.api.dto.ReturnItemRequest;
import com.boticario.modules.purchasing.application.PurchaseService;
import com.boticario.modules.purchasing.domain.PurchaseStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Todas las mutaciones de este controlador requieren PURCHASE_MANAGE
 * (ADMIN/FARMACEUTICO); las consultas quedan abiertas a cualquier usuario
 * autenticado, igual que en catalogo e inventario.
 */
@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public PageResponse<PurchaseSummaryDTO> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) PurchaseStatus status,
            Pageable pageable) {
        return purchaseService.search(search, supplierId, status, pageable);
    }

    @GetMapping("/{id}")
    public PurchaseDTO findById(@PathVariable Long id) {
        return purchaseService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseDTO create(@Valid @RequestBody CreatePurchaseRequest request) {
        return purchaseService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    public PurchaseDTO update(@PathVariable Long id, @Valid @RequestBody CreatePurchaseRequest request) {
        return purchaseService.update(id, request);
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    public PurchaseDTO receive(@PathVariable Long id) {
        return purchaseService.receive(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    public PurchaseDTO cancel(@PathVariable Long id) {
        return purchaseService.cancel(id);
    }

    @PostMapping("/{id}/items/{itemId}/return")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    public PurchaseDTO returnItem(@PathVariable Long id, @PathVariable Long itemId,
                                   @Valid @RequestBody ReturnItemRequest request) {
        return purchaseService.returnItem(id, itemId, request);
    }
}
