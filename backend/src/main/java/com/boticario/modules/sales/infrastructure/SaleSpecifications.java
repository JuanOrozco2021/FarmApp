package com.boticario.modules.sales.infrastructure;

import com.boticario.modules.sales.domain.Sale;
import com.boticario.modules.sales.domain.SaleStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class SaleSpecifications {

    private SaleSpecifications() {
    }

    public static Specification<Sale> codeContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String like = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("saleCode")), like);
    }

    public static Specification<Sale> hasStatus(SaleStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Sale> createdFrom(Instant from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Sale> createdTo(Instant to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<Sale> belongsToCustomer(Long customerId) {
        if (customerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }
}
