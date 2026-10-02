-- Modulo de compras. "suppliers" es por ahora un dato maestro minimo
-- (nombre, NIT, contacto): la relacion comercial completa con proveedores
-- (y clientes) se profundiza en la Fase 7, sin romper lo que se construye
-- aqui. El ciclo de vida de una compra es BORRADOR -> RECIBIDA o ANULADA.
-- Recibir es la UNICA accion de este modulo que toca inventario, y lo hace
-- siempre a traves de StockAllocationService.increaseStock (una llamada
-- por item, dentro de la misma transaccion) — nunca escribiendo las
-- tablas inventory_* directamente, igual que ajustes en la Fase 4.

CREATE TABLE suppliers (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    tax_id      VARCHAR(30),
    phone       VARCHAR(30),
    email       VARCHAR(120),
    address     VARCHAR(255),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(60),
    updated_by  VARCHAR(60),
    CONSTRAINT uq_suppliers_tax_id UNIQUE (tax_id)
);

CREATE SEQUENCE purchase_code_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE purchases (
    id              BIGSERIAL PRIMARY KEY,
    purchase_code   VARCHAR(20)   NOT NULL UNIQUE,
    supplier_id     BIGINT        NOT NULL REFERENCES suppliers(id),
    invoice_number  VARCHAR(60),
    purchase_date   DATE          NOT NULL DEFAULT CURRENT_DATE,
    status          VARCHAR(20)   NOT NULL DEFAULT 'BORRADOR',
    total_amount    NUMERIC(14,2) NOT NULL DEFAULT 0,
    notes           VARCHAR(500),
    received_at     TIMESTAMPTZ,
    received_by     BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      VARCHAR(60),
    updated_by      VARCHAR(60),
    CONSTRAINT chk_purchase_status CHECK (status IN ('BORRADOR','RECIBIDA','ANULADA')),
    CONSTRAINT chk_purchase_total_non_negative CHECK (total_amount >= 0)
);

-- Cada item queda ligado, una vez recibida la compra, al movimiento de
-- kardex exacto que genero (movement_id) — asi una devolucion a proveedor
-- (Fase 5 tambien) sabe de que lote descontar sin adivinar.
CREATE TABLE purchase_items (
    id                      BIGSERIAL PRIMARY KEY,
    purchase_id             BIGINT        NOT NULL REFERENCES purchases(id),
    presentation_id         BIGINT        NOT NULL REFERENCES product_presentations(id),
    quantity_presentation   NUMERIC(14,3) NOT NULL,
    quantity_base           NUMERIC(14,3) NOT NULL,
    unit_cost               NUMERIC(14,2) NOT NULL,
    lot_code                VARCHAR(60),
    expiration_date         DATE,
    movement_id             BIGINT        REFERENCES inventory_movements(id),
    returned_quantity_base  NUMERIC(14,3) NOT NULL DEFAULT 0,
    CONSTRAINT chk_purchase_item_qty_positive CHECK (quantity_presentation > 0 AND quantity_base > 0),
    CONSTRAINT chk_purchase_item_cost_non_negative CHECK (unit_cost >= 0),
    CONSTRAINT chk_purchase_item_returned_range CHECK (returned_quantity_base >= 0 AND returned_quantity_base <= quantity_base)
);

CREATE INDEX idx_suppliers_active ON suppliers (active);
CREATE INDEX idx_purchases_supplier ON purchases (supplier_id);
CREATE INDEX idx_purchases_status ON purchases (status);
CREATE INDEX idx_purchase_items_purchase ON purchase_items (purchase_id);
CREATE INDEX idx_purchase_items_presentation ON purchase_items (presentation_id);
