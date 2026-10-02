package com.boticario.modules.catalog.api.dto;

import com.boticario.modules.catalog.domain.Barcode;

public record BarcodeDTO(Long id, String code, boolean primary) {
    public static BarcodeDTO from(Barcode barcode) {
        return new BarcodeDTO(barcode.getId(), barcode.getCode(), barcode.isPrimary());
    }
}
