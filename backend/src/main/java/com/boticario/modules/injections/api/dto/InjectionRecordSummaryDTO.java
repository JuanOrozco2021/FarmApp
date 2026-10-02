package com.boticario.modules.injections.api.dto;

import com.boticario.modules.injections.domain.InjectionRecord;

import java.math.BigDecimal;
import java.time.Instant;

public record InjectionRecordSummaryDTO(
        Long id,
        String injectionCode,
        String patientName,
        String medicationName,
        Instant appliedAt,
        BigDecimal fee,
        String status
) {
    public static InjectionRecordSummaryDTO from(InjectionRecord record) {
        return new InjectionRecordSummaryDTO(record.getId(), record.getInjectionCode(), record.getPatientName(),
                record.getMedicationName(), record.getAppliedAt(), record.getFee(), record.getStatus().name());
    }
}
