package com.boticario.modules.settings.infrastructure;

import com.boticario.modules.settings.domain.BusinessSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, Short> {
}
