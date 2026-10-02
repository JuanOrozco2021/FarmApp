package com.boticario.modules.purchasing.api;

import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.purchasing.api.dto.SupplierDTO;
import com.boticario.modules.purchasing.api.dto.SupplierRequest;
import com.boticario.modules.purchasing.application.SupplierService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public PageResponse<SupplierDTO> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return supplierService.search(search, active, pageable);
    }

    @GetMapping("/{id}")
    public SupplierDTO findById(@PathVariable Long id) {
        return supplierService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDTO create(@Valid @RequestBody SupplierRequest request) {
        return supplierService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    public SupplierDTO update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PURCHASE_MANAGE')")
    public SupplierDTO setActive(@PathVariable Long id, @RequestParam boolean active) {
        return supplierService.setActive(id, active);
    }
}
