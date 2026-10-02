package com.boticario.modules.catalog.api;

import com.boticario.modules.catalog.api.dto.UnitOptionDTO;
import com.boticario.modules.catalog.infrastructure.UnitRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catalogo fijo de unidades de medida — de solo lectura desde la API (se administra por migracion). */
@RestController
@RequestMapping("/api/units")
public class UnitController {

    private final UnitRepository unitRepository;

    public UnitController(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    @GetMapping
    public List<UnitOptionDTO> findAll() {
        return unitRepository.findAll().stream()
                .map(u -> new UnitOptionDTO(u.getId(), u.getCode(), u.getCode() + " — " + u.getName()))
                .toList();
    }
}
