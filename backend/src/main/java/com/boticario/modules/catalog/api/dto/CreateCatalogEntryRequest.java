package com.boticario.modules.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCatalogEntryRequest(@NotBlank String name) {
}
