package com.boticario.modules.injections.api.dto;

import com.boticario.modules.injections.domain.InjectionRecord;

import java.math.BigDecimal;
import java.time.Instant;

public record InjectionRecordDTO(
        Long id,
        String injectionCode,
        Long customerId,
        String patientName,
        String medicationName,
        String dose,
        String lotCode,
        String applicationSite,
        Long relatedSaleId,
        Instant appliedAt,
        String observations,
        BigDecimal fee,
        String paymentMethod,
        String status,
        Instant cancelledAt,
        String cancelledReason
) {
    public static InjectionRecordDTO from(InjectionRecord record) {
        return new InjectionRecordDTO(
                record.getId(), record.getInjectionCode(),
                record.getCustomer() != null ? record.getCustomer().getId() : null,
                record.getPatientName(), record.getMedicationName(), record.getDose(), record.getLotCode(),
                record.getApplicationSite(), record.getRelatedSaleId(), record.getAppliedAt(),
                record.getObservations(), record.getFee(), record.getPaymentMethod().name(),
                record.getStatus().name(), record.getCancelledAt(), record.getCancelledReason()
        );
    }
}
