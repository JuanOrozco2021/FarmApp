package com.boticario.modules.audit.infrastructure;

import com.boticario.common.audit.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> hasEntityName(String entityName) {
        if (entityName == null || entityName.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("entityName"), entityName);
    }

    public static Specification<AuditLog> hasAction(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> hasUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("username"), username);
    }

    public static Specification<AuditLog> occurredBetween(Instant from, Instant to) {
        if (from == null || to == null) {
            return null;
        }
        return (root, query, cb) -> cb.between(root.get("occurredAt"), from, to);
    }
}
