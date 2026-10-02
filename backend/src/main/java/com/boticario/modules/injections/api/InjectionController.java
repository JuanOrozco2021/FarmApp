package com.boticario.modules.injections.api;

import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.injections.api.dto.CancelInjectionRequest;
import com.boticario.modules.injections.api.dto.InjectionRecordDTO;
import com.boticario.modules.injections.api.dto.InjectionRecordRequest;
import com.boticario.modules.injections.api.dto.InjectionRecordSummaryDTO;
import com.boticario.modules.injections.application.InjectionService;
import com.boticario.modules.injections.domain.InjectionStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Registrar un servicio requiere INJECTION_REGISTER (INYECTOLOGIA,
 * FARMACEUTICO y ADMIN la tienen). Anular requiere INJECTION_CANCEL
 * (INYECTOLOGIA NO la tiene — quien aplica no anula su propio cobro,
 * igual criterio que CAJERO/SALE_CANCEL en la Fase 6). Las consultas
 * quedan abiertas a cualquier usuario autenticado.
 */
@RestController
@RequestMapping("/api/injections")
public class InjectionController {

    private final InjectionService injectionService;

    public InjectionController(InjectionService injectionService) {
        this.injectionService = injectionService;
    }

    @GetMapping
    public PageResponse<InjectionRecordSummaryDTO> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) InjectionStatus status,
            @RequestParam(required = false) Long customerId,
            Pageable pageable) {
        return injectionService.search(search, status, customerId, pageable);
    }

    @GetMapping("/{id}")
    public InjectionRecordDTO findById(@PathVariable Long id) {
        return injectionService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INJECTION_REGISTER')")
    @ResponseStatus(HttpStatus.CREATED)
    public InjectionRecordDTO create(@Valid @RequestBody InjectionRecordRequest request) {
        return injectionService.create(request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('INJECTION_CANCEL')")
    public InjectionRecordDTO cancel(@PathVariable Long id, @Valid @RequestBody CancelInjectionRequest request) {
        return injectionService.cancel(id, request);
    }
}
