package com.boticario.modules.settings.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Fila unica de configuracion del negocio (id siempre = 1). */
@Entity
@Table(name = "business_settings")
@Getter
@Setter
@NoArgsConstructor
public class BusinessSettings {

    @Id
    private Short id = 1;

    @Column(name = "pharmacy_name", nullable = false, length = 150)
    private String pharmacyName;

    @Column(length = 30)
    private String nit;

    @Column(length = 255)
    private String address;

    @Column(name = "expiration_alert_days", nullable = false)
    private Integer expirationAlertDays;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by", length = 60)
    private String updatedBy;
}
