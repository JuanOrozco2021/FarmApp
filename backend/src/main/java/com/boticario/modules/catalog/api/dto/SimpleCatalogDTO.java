package com.boticario.modules.catalog.api.dto;

/** DTO generico para catalogos auxiliares de solo lectura (categoria, laboratorio, principio activo, unidad). */
public record SimpleCatalogDTO(Long id, String name) {
}
