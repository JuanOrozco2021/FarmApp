package com.boticario.modules.catalog.infrastructure;

import com.boticario.modules.catalog.domain.Product;
import com.boticario.modules.catalog.domain.ProductStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros de busqueda del listado de productos (regla 30: busqueda por
 * nombre, categoria, laboratorio, principio activo). Se combinan con
 * Specification.where(...).and(...) sin acoplar el repositorio a cada
 * combinacion posible de filtros.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> nameContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String like = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("internalCode")), like)
        );
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> hasLaboratory(Long laboratoryId) {
        if (laboratoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("laboratory").get("id"), laboratoryId);
    }

    public static Specification<Product> hasActiveIngredient(Long activeIngredientId) {
        if (activeIngredientId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("activeIngredient").get("id"), activeIngredientId);
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
