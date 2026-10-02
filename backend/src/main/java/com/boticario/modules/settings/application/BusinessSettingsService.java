package com.boticario.modules.settings.application;

import com.boticario.common.audit.Audited;
import com.boticario.common.audit.CurrentUserProvider;
import com.boticario.modules.settings.api.dto.BusinessSettingsDTO;
import com.boticario.modules.settings.api.dto.UpdateBusinessSettingsRequest;
import com.boticario.modules.settings.domain.BusinessSettings;
import com.boticario.modules.settings.infrastructure.BusinessSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class BusinessSettingsService {

    private final BusinessSettingsRepository repository;
    private final CurrentUserProvider currentUserProvider;

    public BusinessSettingsService(BusinessSettingsRepository repository, CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public BusinessSettingsDTO get() {
        return BusinessSettingsDTO.from(load());
    }

    @Transactional
    @Audited(action = "SETTINGS_UPDATE", entity = "BusinessSettings")
    public BusinessSettingsDTO update(UpdateBusinessSettingsRequest request) {
        BusinessSettings settings = load();
        settings.setPharmacyName(request.pharmacyName());
        settings.setNit(request.nit());
        settings.setAddress(request.address());
        settings.setExpirationAlertDays(request.expirationAlertDays());
        settings.setUpdatedAt(Instant.now());
        settings.setUpdatedBy(currentUserProvider.currentUsername());
        return BusinessSettingsDTO.from(repository.save(settings));
    }

    private BusinessSettings load() {
        return repository.findById((short) 1)
                .orElseThrow(() -> new IllegalStateException(
                        "La fila de configuracion del negocio no existe. Verifica que la migracion V5/V6 se haya aplicado."));
    }
}
