package com.boticario.modules.catalog.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String name,
        Long activeIngredientId,
        String concentration,
        String pharmaceuticalForm,
        Long categoryId,
        Long laboratoryId,
        String brand,
        String description,
        @NotBlank String baseUnitCode,
        BigDecimal minStockBase,
        BigDecimal maxStockBase,
        boolean requiresPrescription,
        boolean fractionable,
        boolean controlsLot,
        boolean controlsExpiration,
        /** Presentacion base (factor 1 en baseUnitCode). Es obligatoria: todo producto nace con al menos una presentacion vendible. */
        @NotNull @Valid PresentationRequest basePresentation
) {
}
