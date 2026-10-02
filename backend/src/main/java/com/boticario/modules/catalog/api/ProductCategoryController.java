package com.boticario.modules.catalog.api;

import com.boticario.modules.catalog.api.dto.CreateCatalogEntryRequest;
import com.boticario.modules.catalog.api.dto.SimpleCatalogDTO;
import com.boticario.modules.catalog.application.ProductCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class ProductCategoryController {

    private final ProductCategoryService service;

    public ProductCategoryController(ProductCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<SimpleCatalogDTO> findAll() {
        return service.findAllActive();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public SimpleCatalogDTO create(@Valid @RequestBody CreateCatalogEntryRequest request) {
        return service.create(request);
    }
}
