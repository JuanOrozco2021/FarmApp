package com.boticario.modules.catalog.api;

import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.catalog.api.dto.*;
import com.boticario.modules.catalog.application.ProductService;
import com.boticario.modules.catalog.application.PresentationService;
import com.boticario.modules.catalog.domain.ProductStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final PresentationService presentationService;

    public ProductController(ProductService productService, PresentationService presentationService) {
        this.productService = productService;
        this.presentationService = presentationService;
    }

    @GetMapping
    public PageResponse<ProductSummaryDTO> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long activeIngredientId,
            @RequestParam(required = false) ProductStatus status,
            Pageable pageable) {
        return productService.search(search, categoryId, laboratoryId, activeIngredientId, status, pageable);
    }

    @GetMapping("/{id}")
    public ProductDTO findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @GetMapping("/barcode/{code}")
    public BarcodeResolutionDTO resolveBarcode(@PathVariable String code) {
        return presentationService.resolveBarcode(code);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDTO create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    public ProductDTO update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    public ProductDTO setStatus(@PathVariable Long id, @RequestParam ProductStatus status) {
        return productService.setStatus(id, status);
    }

    @PostMapping("/{id}/presentations")
    @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public PresentationDTO addPresentation(@PathVariable Long id, @Valid @RequestBody PresentationRequest request) {
        return productService.addPresentation(id, request);
    }
}
