-- Fase 7: dato maestro de clientes (nivel minimo, igual criterio que
-- suppliers en la Fase 5: nombre + documento + contacto, sin credito ni
-- saldo pendiente por ahora — eso es una funcionalidad completa aparte).
-- La venta se sigue pudiendo registrar sin cliente (mostrador/anonima):
-- customer_id es NULLABLE y sales.customer_name (Fase 6) se mantiene como
-- snapshot de texto libre, tal como quedo documentado en Sale.java.

CREATE TABLE customers (
    id               BIGSERIAL PRIMARY KEY,
    document_type    VARCHAR(20),
    document_number  VARCHAR(30),
    name             VARCHAR(150) NOT NULL,
    phone            VARCHAR(30),
    email            VARCHAR(120),
    address          VARCHAR(255),
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       VARCHAR(60),
    updated_by       VARCHAR(60),
    CONSTRAINT chk_customer_document_type CHECK (document_type IS NULL OR document_type IN ('CC','CE','TI','PASAPORTE','NIT'))
);

-- Documento unico solo cuando se informa (muchos clientes de mostrador se
-- registran solo con nombre y telefono, sin documento).
CREATE UNIQUE INDEX uq_customers_document_number ON customers (document_number) WHERE document_number IS NOT NULL;
CREATE INDEX idx_customers_active ON customers (active);

ALTER TABLE sales ADD COLUMN customer_id BIGINT REFERENCES customers(id);
CREATE INDEX idx_sales_customer ON sales (customer_id);

-- Enriquecer el dato maestro de proveedor (Fase 5 ya anunciaba esto).
-- Deliberadamente NO se agrega cuentas por pagar/saldo por factura aqui:
-- payment_terms_days es solo informativo (dias de credito pactados con el
-- proveedor), no se valida ni se lleva saldo contra el — esa es una
-- funcionalidad completa en si misma, igual que el fiado de clientes.
ALTER TABLE suppliers ADD COLUMN contact_name VARCHAR(150);
ALTER TABLE suppliers ADD COLUMN payment_terms_days INTEGER;
ALTER TABLE suppliers ADD COLUMN notes VARCHAR(500);
ALTER TABLE suppliers ADD CONSTRAINT chk_supplier_payment_terms_non_negative CHECK (payment_terms_days IS NULL OR payment_terms_days >= 0);
