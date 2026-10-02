package com.boticario.modules.settings.api;

import com.boticario.modules.settings.api.dto.BusinessSettingsDTO;
import com.boticario.modules.settings.api.dto.UpdateBusinessSettingsRequest;
import com.boticario.modules.settings.application.BusinessSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Configuracion global del negocio (nombre de la farmacia, dias de alerta de vencimiento, etc). */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final BusinessSettingsService settingsService;

    public SettingsController(BusinessSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public BusinessSettingsDTO get() {
        return settingsService.get();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public BusinessSettingsDTO update(@Valid @RequestBody UpdateBusinessSettingsRequest request) {
        return settingsService.update(request);
    }
}
