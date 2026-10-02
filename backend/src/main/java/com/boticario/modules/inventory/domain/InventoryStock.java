package com.boticario.modules.inventory.domain;

import com.boticario.modules.catalog.domain.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;

/**
 * Fuente unica de verdad del stock de un producto. Nunca se escribe aqui
 * sin pasar por StockAllocationService, que la mantiene sincronizada con
 * inventory_lots y siempre deja un inventory_movement como evidencia.
 * @Version habilita bloqueo optimista como respaldo del SELECT ... FOR
 * UPDATE que StockAllocationService toma explicitamente (regla 24: concurrencia).
 * <p>
 * Implementa {@link Persistable} a proposito: el id ({@code productId}) no
 * lo genera la base de datos, se copia del Product asociado (@MapsId) y
 * por lo tanto NUNCA es null, ni siquiera para una fila que todavia no
 * existe en inventory_stock. Sin esto, Spring Data JPA interpreta
 * "id != null" como "ya existe" y llama a EntityManager.merge() en vez de
 * persist() para la primera fila de stock de un producto -- eso es
 * exactamente lo que producia el error de Hibernate
 * "AssertionFailure: null identifier (InventoryStock)" al recibir la
 * primera compra de un producto sin stock previo: merge() intenta
 * resolver una entidad @OneToOne/@MapsId que Hibernate nunca cargo en la
 * sesion. isNew() explicito le dice a Spring Data "esto es un persist(),
 * no un merge()" para toda instancia recien construida.
 */
@Entity
@Table(name = "inventory_stock")
@Getter
@Setter
@NoArgsConstructor
public class InventoryStock implements Persistable<Long> {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity_base", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityBase = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Transient
    private boolean newEntity = true;

    public InventoryStock(Product product) {
        this.product = product;
        this.productId = product.getId();
        this.quantityBase = BigDecimal.ZERO;
    }

    @Override
    public Long getId() {
        return productId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }
}
