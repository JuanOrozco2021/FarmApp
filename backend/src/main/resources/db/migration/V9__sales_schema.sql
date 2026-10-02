-- Modulo de ventas (POS). Una venta se registra en UNA sola transaccion
-- (a diferencia de compras, no hay estado BORRADOR: el cajero cobra en el
-- momento). El descuento de stock siempre es por FEFO
-- (StockAllocationService.decreaseStockFefo) — el cajero nunca elige el
-- lote — y cada lote realmente consumido queda registrado en
-- sale_item_lots, para poder revertir la venta exactamente si se anula
-- (regla 10: nunca stock negativo; regla 13: kardex inmutable, una
-- anulacion genera movimientos DEVOLUCION_VENTA nuevos, no borra nada).

CREATE SEQUENCE sale_code_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE sales (
    id                BIGSERIAL PRIMARY KEY,
    sale_code         VARCHAR(20)   NOT NULL UNIQUE,
    customer_name     VARCHAR(150),
    subtotal          NUMERIC(14,2) NOT NULL DEFAULT 0,
    discount_total    NUMERIC(14,2) NOT NULL DEFAULT 0,
    total             NUMERIC(14,2) NOT NULL DEFAULT 0,
    change_amount     NUMERIC(14,2) NOT NULL DEFAULT 0,
    status            VARCHAR(20)   NOT NULL DEFAULT 'ACTIVA',
    cancelled_at      TIMESTAMPTZ,
    cancelled_reason  VARCHAR(255),
    cancelled_by      BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        VARCHAR(60),
    updated_by        VARCHAR(60),
    CONSTRAINT chk_sale_status CHECK (status IN ('ACTIVA','ANULADA')),
    CONSTRAINT chk_sale_amounts_non_negative CHECK (subtotal >= 0 AND discount_total >= 0 AND total >= 0 AND change_amount >= 0)
);

CREATE TABLE sale_items (
    id                     BIGSERIAL PRIMARY KEY,
    sale_id                BIGINT        NOT NULL REFERENCES sales(id),
    presentation_id        BIGINT        NOT NULL REFERENCES product_presentations(id),
    quantity_presentation  NUMERIC(14,3) NOT NULL,
    quantity_base          NUMERIC(14,3) NOT NULL,
    unit_price             NUMERIC(14,2) NOT NULL,
    discount_percentage    NUMERIC(5,2)  NOT NULL DEFAULT 0,
    line_total             NUMERIC(14,2) NOT NULL,
    requires_prescription  BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_sale_item_qty_positive CHECK (quantity_presentation > 0 AND quantity_base > 0),
    CONSTRAINT chk_sale_item_price_non_negative CHECK (unit_price >= 0 AND line_total >= 0),
    CONSTRAINT chk_sale_item_discount_range CHECK (discount_percentage >= 0 AND discount_percentage <= 100)
);

-- Un item puede haber tomado de mas de un lote (FEFO puede repartir). Al
-- anular la venta, cada fila aqui dice exactamente a que lote reponer
-- cuanto, sin tener que adivinar ni volver a correr FEFO al reves.
CREATE TABLE sale_item_lots (
    id              BIGSERIAL PRIMARY KEY,
    sale_item_id    BIGINT        NOT NULL REFERENCES sale_items(id),
    lot_id          BIGINT        NOT NULL REFERENCES inventory_lots(id),
    quantity_base   NUMERIC(14,3) NOT NULL,
    movement_id     BIGINT        REFERENCES inventory_movements(id),
    CONSTRAINT chk_sale_item_lot_qty_positive CHECK (quantity_base > 0)
);

CREATE TABLE sale_payments (
    id              BIGSERIAL PRIMARY KEY,
    sale_id         BIGINT        NOT NULL REFERENCES sales(id),
    payment_method  VARCHAR(20)   NOT NULL,
    amount          NUMERIC(14,2) NOT NULL,
    CONSTRAINT chk_sale_payment_method CHECK (payment_method IN ('EFECTIVO','TARJETA','TRANSFERENCIA')),
    CONSTRAINT chk_sale_payment_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_sales_status ON sales (status);
CREATE INDEX idx_sales_created_at ON sales (created_at);
CREATE INDEX idx_sale_items_sale ON sale_items (sale_id);
CREATE INDEX idx_sale_items_presentation ON sale_items (presentation_id);
CREATE INDEX idx_sale_item_lots_item ON sale_item_lots (sale_item_id);
CREATE INDEX idx_sale_item_lots_lot ON sale_item_lots (lot_id);
CREATE INDEX idx_sale_payments_sale ON sale_payments (sale_id);
