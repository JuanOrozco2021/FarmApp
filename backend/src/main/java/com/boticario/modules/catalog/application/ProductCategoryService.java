package com.boticario.modules.catalog.application;

import com.boticario.common.audit.Audited;
import com.boticario.common.exception.DuplicateResourceException;
import com.boticario.modules.catalog.api.dto.CreateCatalogEntryRequest;
import com.boticario.modules.catalog.api.dto.SimpleCatalogDTO;
import com.boticario.modules.catalog.domain.ProductCategory;
import com.boticario.modules.catalog.infrastructure.ProductCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductCategoryService {

    private final ProductCategoryRepository repository;

    public ProductCategoryService(ProductCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SimpleCatalogDTO> findAllActive() {
        return repository.findAll().stream()
                .filter(ProductCategory::isActive)
                .map(c -> new SimpleCatalogDTO(c.getId(), c.getName()))
                .toList();
    }

    @Transactional
    @Audited(action = "CATALOG_CATEGORY_CREATE", entity = "ProductCategory")
    public SimpleCatalogDTO create(CreateCatalogEntryRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Ya existe la categoria " + request.name());
        }
        ProductCategory category = new ProductCategory();
        category.setName(request.name());
        category = repository.save(category);
        return new SimpleCatalogDTO(category.getId(), category.getName());
    }
}
