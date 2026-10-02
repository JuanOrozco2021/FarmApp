package com.boticario.modules.purchasing.infrastructure;

import com.boticario.modules.purchasing.domain.Purchase;
import com.boticario.modules.purchasing.domain.PurchaseStatus;
import org.springframework.data.jpa.domain.Specification;

public final class PurchaseSpecifications {

    private PurchaseSpecifications() {
    }

    public static Specification<Purchase> hasSupplier(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("supplier").get("id"), supplierId);
    }

    public static Specification<Purchase> hasStatus(PurchaseStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Purchase> codeOrInvoiceContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String like = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("purchaseCode")), like),
                cb.like(cb.lower(cb.coalesce(root.get("invoiceNumber"), "")), like)
        );
    }
}
