package com.boticario.modules.catalog.application;

import com.boticario.common.audit.Audited;
import com.boticario.common.exception.DuplicateResourceException;
import com.boticario.modules.catalog.api.dto.CreateCatalogEntryRequest;
import com.boticario.modules.catalog.api.dto.SimpleCatalogDTO;
import com.boticario.modules.catalog.domain.ActiveIngredient;
import com.boticario.modules.catalog.infrastructure.ActiveIngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ActiveIngredientService {

    private final ActiveIngredientRepository repository;

    public ActiveIngredientService(ActiveIngredientRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SimpleCatalogDTO> findAllActive() {
        return repository.findAll().stream()
                .filter(ActiveIngredient::isActive)
                .map(i -> new SimpleCatalogDTO(i.getId(), i.getName()))
                .toList();
    }

    @Transactional
    @Audited(action = "CATALOG_ACTIVE_INGREDIENT_CREATE", entity = "ActiveIngredient")
    public SimpleCatalogDTO create(CreateCatalogEntryRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Ya existe el principio activo " + request.name());
        }
        ActiveIngredient ingredient = new ActiveIngredient();
        ingredient.setName(request.name());
        ingredient = repository.save(ingredient);
        return new SimpleCatalogDTO(ingredient.getId(), ingredient.getName());
    }
}
