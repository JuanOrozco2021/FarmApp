package com.boticario.modules.injections.infrastructure;

import com.boticario.modules.injections.domain.InjectionRecord;
import com.boticario.modules.injections.domain.InjectionStatus;
import org.springframework.data.jpa.domain.Specification;

public final class InjectionRecordSpecifications {

    private InjectionRecordSpecifications() {
    }

    public static Specification<InjectionRecord> codeOrPatientContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String like = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("injectionCode")), like),
                cb.like(cb.lower(root.get("patientName")), like)
        );
    }

    public static Specification<InjectionRecord> hasStatus(InjectionStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<InjectionRecord> belongsToCustomer(Long customerId) {
        if (customerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }
}
