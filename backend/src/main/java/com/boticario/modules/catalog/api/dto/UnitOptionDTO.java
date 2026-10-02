package com.boticario.modules.catalog.api.dto;

/**
 * DTO especifico para el selector de unidades de medida.
 * A diferencia de {@link SimpleCatalogDTO} (usado por categoria, laboratorio
 * y principio activo, donde el frontend referencia la fila por su {@code id}
 * numerico), aqui el backend necesita el {@code code} real (ej. "UNIDAD")
 * porque {@code baseUnitCode}/{@code unitCode} viajan como codigo de negocio,
 * no como id. {@code name} es solo la etiqueta combinada para mostrar en
 * el dropdown (ej. "UNIDAD — Unidad").
 */
public record UnitOptionDTO(Long id, String code, String name) {
}
