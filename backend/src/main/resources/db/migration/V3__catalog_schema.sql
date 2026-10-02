-- Modulo de catalogo: unidades, productos, presentaciones, codigos de barras
-- y precios. Cadena critica: producto -> presentacion -> unidad base ->
-- codigo de barras -> (lote/inventario se agrega en la Fase 4).

CREATE TABLE units (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(20)  NOT NULL UNIQUE,
    name VARCHAR(60)  NOT NULL
);

CREATE TABLE product_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE laboratories (
    id     BIGSERIAL PRIMARY KEY,
    name   VARCHAR(150) NOT NULL UNIQUE,
    active BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE active_ingredients (
    id     BIGSERIAL PRIMARY KEY,
    name   VARCHAR(150) NOT NULL UNIQUE,
    active BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Secuencia propia para el codigo interno legible (ej. PRD-000123).
-- El ID interno real (PK) sigue siendo el bigserial; esto es solo
-- un identificador amigable, nunca el codigo de barras.
CREATE SEQUENCE product_internal_code_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE products (
    id                     BIGSERIAL PRIMARY KEY,
    internal_code          VARCHAR(20)   NOT NULL UNIQUE,
    name                   VARCHAR(200)  NOT NULL,
    active_ingredient_id   BIGINT        REFERENCES active_ingredients(id),
    concentration          VARCHAR(60),
    pharmaceutical_form    VARCHAR(60),
    category_id            BIGINT        REFERENCES product_categories(id),
    laboratory_id          BIGINT        REFERENCES laboratories(id),
    brand                  VARCHAR(100),
    description            VARCHAR(500),
    base_unit_id           BIGINT        NOT NULL REFERENCES units(id),
    min_stock_base         NUMERIC(14,3) NOT NULL DEFAULT 0,
    max_stock_base         NUMERIC(14,3),
    requires_prescription  BOOLEAN       NOT NULL DEFAULT FALSE,
    is_fractionable        BOOLEAN       NOT NULL DEFAULT TRUE,
    controls_lot           BOOLEAN       NOT NULL DEFAULT TRUE,
    controls_expiration    BOOLEAN       NOT NULL DEFAULT TRUE,
    status                 VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by             VARCHAR(60),
    updated_by             VARCHAR(60),
    CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE','INACTIVE')),
    CONSTRAINT chk_products_min_stock CHECK (min_stock_base >= 0)
);

CREATE TABLE product_presentations (
    id                    BIGSERIAL PRIMARY KEY,
    product_id            BIGINT        NOT NULL REFERENCES products(id),
    unit_id               BIGINT        NOT NULL REFERENCES units(id),
    conversion_factor     NUMERIC(14,6) NOT NULL,
    sale_price            NUMERIC(14,2) NOT NULL,
    purchase_price        NUMERIC(14,2),
    is_base_presentation  BOOLEAN       NOT NULL DEFAULT FALSE,
    active                BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_presentation_factor CHECK (conversion_factor > 0),
    CONSTRAINT chk_presentation_price CHECK (sale_price >= 0)
);

-- Solo puede existir una presentacion base (factor 1) activa por producto.
CREATE UNIQUE INDEX uq_product_base_presentation
    ON product_presentations (product_id)
    WHERE is_base_presentation = TRUE AND active = TRUE;

CREATE TABLE barcodes (
    id              BIGSERIAL PRIMARY KEY,
    presentation_id BIGINT      NOT NULL REFERENCES product_presentations(id),
    code            VARCHAR(64) NOT NULL,
    is_primary      BOOLEAN     NOT NULL DEFAULT TRUE,
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Unico entre los codigos activos: permite reasignar un codigo si su
-- barcode anterior fue desactivado (ej. reempaque del laboratorio),
-- sin perder el historial de a que presentacion perteneció.
CREATE UNIQUE INDEX uq_barcodes_code_active
    ON barcodes (code)
    WHERE active = TRUE;

CREATE TABLE price_history (
    id              BIGSERIAL PRIMARY KEY,
    presentation_id BIGINT        NOT NULL REFERENCES product_presentations(id),
    old_price       NUMERIC(14,2) NOT NULL,
    new_price       NUMERIC(14,2) NOT NULL,
    changed_by      VARCHAR(60),
    changed_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    reason          VARCHAR(255)
);

-- Indices de busqueda (regla 30 del encargo: codigo de barras, nombre,
-- codigo, principio activo, categoria).
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_products_name_trgm ON products USING gin (name gin_trgm_ops);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_laboratory ON products (laboratory_id);
CREATE INDEX idx_products_active_ingredient ON products (active_ingredient_id);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_presentations_product ON product_presentations (product_id);
CREATE INDEX idx_barcodes_presentation ON barcodes (presentation_id);
CREATE INDEX idx_price_history_presentation ON price_history (presentation_id);
