package com.boticario.modules.customers.api;

import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.customers.api.dto.CustomerDTO;
import com.boticario.modules.customers.api.dto.CustomerRequest;
import com.boticario.modules.customers.application.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Las consultas quedan abiertas a cualquier usuario autenticado (igual
 * que Suppliers y Sales): cualquiera que atienda mostrador necesita poder
 * buscar un cliente para ligarlo a una venta. Crear/editar/desactivar
 * requiere CUSTOMER_MANAGE (ADMIN, FARMACEUTICO y CAJERO la tienen — es
 * solo dato de contacto, bajo riesgo).
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public PageResponse<CustomerDTO> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return customerService.search(search, active, pageable);
    }

    @GetMapping("/{id}")
    public CustomerDTO findById(@PathVariable Long id) {
        return customerService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDTO create(@Valid @RequestBody CustomerRequest request) {
        return customerService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    public CustomerDTO update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    public CustomerDTO setActive(@PathVariable Long id, @RequestParam boolean active) {
        return customerService.setActive(id, active);
    }
}
