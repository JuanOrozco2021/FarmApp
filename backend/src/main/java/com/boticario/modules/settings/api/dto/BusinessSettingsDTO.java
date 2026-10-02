package com.boticario.modules.settings.api.dto;

import com.boticario.modules.settings.domain.BusinessSettings;

public record BusinessSettingsDTO(String pharmacyName, String nit, String address, Integer expirationAlertDays) {
    public static BusinessSettingsDTO from(BusinessSettings settings) {
        return new BusinessSettingsDTO(
                settings.getPharmacyName(), settings.getNit(), settings.getAddress(), settings.getExpirationAlertDays());
    }
}
