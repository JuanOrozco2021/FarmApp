package com.boticario.modules.purchasing.infrastructure;

import com.boticario.modules.purchasing.domain.Supplier;
import org.springframework.data.jpa.domain.Specification;

public final class SupplierSpecifications {

    private SupplierSpecifications() {
    }

    public static Specification<Supplier> nameOrTaxIdContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String like = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(cb.coalesce(root.get("taxId"), "")), like)
        );
    }

    public static Specification<Supplier> isActive(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
