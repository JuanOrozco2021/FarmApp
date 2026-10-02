-- Fase 8: servicio de inyectologia. Decision confirmada con el usuario:
-- el medicamento SIEMPRE lo trae el paciente (o se compra aparte con una
-- venta normal del POS) — este modulo NUNCA descuenta inventario, solo
-- registra el servicio de aplicacion y cobra su tarifa. Por eso
-- medication_name/lot_code son texto libre (no referencian el catalogo
-- ni inventory_lots): no hay nada que validar contra el stock.
-- related_sale_id es una simple anotacion opcional para dejar trazado
-- que el medicamento se compro en esta misma atencion — no fusiona los
-- dos flujos ni afecta al calculo de ninguno de los dos.

CREATE SEQUENCE injection_code_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE injection_records (
    id                  BIGSERIAL PRIMARY KEY,
    injection_code      VARCHAR(20)   NOT NULL UNIQUE,
    customer_id         BIGINT        REFERENCES customers(id),
    patient_name        VARCHAR(150)  NOT NULL,
    medication_name     VARCHAR(150)  NOT NULL,
    dose                VARCHAR(60),
    lot_code            VARCHAR(60),
    application_site    VARCHAR(100),
    related_sale_id     BIGINT        REFERENCES sales(id),
    applied_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    applied_by          BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    observations        VARCHAR(500),
    fee                 NUMERIC(14,2) NOT NULL DEFAULT 0,
    payment_method      VARCHAR(20)   NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVA',
    cancelled_at        TIMESTAMPTZ,
    cancelled_reason    VARCHAR(255),
    cancelled_by        BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          VARCHAR(60),
    updated_by          VARCHAR(60),
    CONSTRAINT chk_injection_status CHECK (status IN ('ACTIVA','ANULADA')),
    CONSTRAINT chk_injection_payment_method CHECK (payment_method IN ('EFECTIVO','TARJETA','TRANSFERENCIA')),
    CONSTRAINT chk_injection_fee_non_negative CHECK (fee >= 0)
);

CREATE INDEX idx_injection_records_status ON injection_records (status);
CREATE INDEX idx_injection_records_applied_at ON injection_records (applied_at);
CREATE INDEX idx_injection_records_customer ON injection_records (customer_id);
