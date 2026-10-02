package com.boticario.modules.catalog.application;

import com.boticario.common.audit.Audited;
import com.boticario.common.exception.DuplicateResourceException;
import com.boticario.modules.catalog.api.dto.CreateCatalogEntryRequest;
import com.boticario.modules.catalog.api.dto.SimpleCatalogDTO;
import com.boticario.modules.catalog.domain.Laboratory;
import com.boticario.modules.catalog.infrastructure.LaboratoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LaboratoryService {

    private final LaboratoryRepository repository;

    public LaboratoryService(LaboratoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SimpleCatalogDTO> findAllActive() {
        return repository.findAll().stream()
                .filter(Laboratory::isActive)
                .map(l -> new SimpleCatalogDTO(l.getId(), l.getName()))
                .toList();
    }

    @Transactional
    @Audited(action = "CATALOG_LABORATORY_CREATE", entity = "Laboratory")
    public SimpleCatalogDTO create(CreateCatalogEntryRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Ya existe el laboratorio " + request.name());
        }
        Laboratory laboratory = new Laboratory();
        laboratory.setName(request.name());
        laboratory = repository.save(laboratory);
        return new SimpleCatalogDTO(laboratory.getId(), laboratory.getName());
    }
}
