-- Modulo de inventario: fuente unica de verdad del stock (inventory_stock),
-- detalle por lote para FEFO (inventory_lots) y kardex inmutable
-- (inventory_movements). Ninguna de las tres se modifica sin las otras dos
-- en la misma transaccion (regla central del encargo).

CREATE TABLE inventory_stock (
    product_id     BIGINT PRIMARY KEY REFERENCES products(id),
    quantity_base  NUMERIC(14,3) NOT NULL DEFAULT 0,
    version        BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT chk_inventory_stock_non_negative CHECK (quantity_base >= 0)
);

CREATE TABLE inventory_lots (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT        NOT NULL REFERENCES products(id),
    lot_code        VARCHAR(60)   NOT NULL,
    expiration_date DATE,
    quantity_base   NUMERIC(14,3) NOT NULL DEFAULT 0,
    received_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_inventory_lots_product_code UNIQUE (product_id, lot_code),
    CONSTRAINT chk_inventory_lots_non_negative CHECK (quantity_base >= 0)
);

CREATE TABLE inventory_movements (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT        NOT NULL REFERENCES products(id),
    lot_id          BIGINT        REFERENCES inventory_lots(id),
    movement_type   VARCHAR(30)   NOT NULL,
    quantity_base   NUMERIC(14,3) NOT NULL,
    balance_after   NUMERIC(14,3) NOT NULL,
    reference_type  VARCHAR(40),
    reference_id    BIGINT,
    user_id         BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    occurred_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    notes           VARCHAR(255),
    CONSTRAINT chk_inventory_movement_type CHECK (movement_type IN (
        'COMPRA','VENTA','DEVOLUCION_COMPRA','DEVOLUCION_VENTA',
        'AJUSTE_POSITIVO','AJUSTE_NEGATIVO','MERMA','VENCIMIENTO','TRASLADO'
    ))
);

CREATE TABLE inventory_adjustments (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT        NOT NULL REFERENCES products(id),
    adjustment_type VARCHAR(20)   NOT NULL,
    quantity_base   NUMERIC(14,3) NOT NULL,
    reason          VARCHAR(255)  NOT NULL,
    movement_id     BIGINT        NOT NULL REFERENCES inventory_movements(id),
    user_id         BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_inventory_adjustment_type CHECK (adjustment_type IN ('POSITIVO','NEGATIVO','MERMA','VENCIMIENTO')),
    CONSTRAINT chk_inventory_adjustment_qty_positive CHECK (quantity_base > 0)
);

-- Configuracion del negocio (fila unica). Se amplia en fases posteriores
-- (moneda, impuestos, datos fiscales) sin romper lo ya construido.
CREATE TABLE business_settings (
    id                     SMALLINT PRIMARY KEY DEFAULT 1,
    pharmacy_name          VARCHAR(150) NOT NULL DEFAULT 'Boticario',
    expiration_alert_days  INTEGER      NOT NULL DEFAULT 30,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by             VARCHAR(60),
    CONSTRAINT chk_business_settings_single_row CHECK (id = 1),
    CONSTRAINT chk_expiration_alert_days_positive CHECK (expiration_alert_days > 0)
);

CREATE INDEX idx_inventory_lots_product ON inventory_lots (product_id);
CREATE INDEX idx_inventory_lots_expiration ON inventory_lots (expiration_date);
CREATE INDEX idx_inventory_movements_product ON inventory_movements (product_id);
CREATE INDEX idx_inventory_movements_occurred_at ON inventory_movements (occurred_at);
CREATE INDEX idx_inventory_movements_reference ON inventory_movements (reference_type, reference_id);
