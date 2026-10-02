-- Catalogo de unidades de medida (regla 7 del encargo).
INSERT INTO units (code, name) VALUES
    ('UNIDAD',     'Unidad'),
    ('PASTILLA',   'Pastilla'),
    ('TABLETA',    'Tableta'),
    ('CAPSULA',    'Capsula'),
    ('FRASCO',     'Frasco'),
    ('CAJA',       'Caja'),
    ('BLISTER',    'Blister'),
    ('AMPOLLA',    'Ampolla'),
    ('SOBRE',      'Sobre'),
    ('MILILITRO',  'Mililitro'),
    ('GRAMO',      'Gramo'),
    ('KILOGRAMO',  'Kilogramo');

-- Datos de ejemplo minimos para poder probar el flujo de punta a punta
-- (regla del encargo: "datos iniciales de prueba"). No son datos reales
-- de ningun laboratorio ni producto comercial.
INSERT INTO product_categories (name, description) VALUES
    ('Analgesicos', 'Medicamentos para el manejo del dolor'),
    ('Antiinflamatorios', 'Medicamentos antiinflamatorios no esteroideos');

INSERT INTO laboratories (name) VALUES ('Laboratorio Generico S.A.');

INSERT INTO active_ingredients (name) VALUES ('Ibuprofeno'), ('Acetaminofen');

-- Producto de ejemplo: el mismo caso trabajado en el documento de arquitectura.
-- Unidad base = PASTILLA. Presentaciones: PASTILLA (factor 1), TABLETA/blister
-- de 10 (factor 10) y CAJA de 100 (factor 100), cada una con su propio codigo
-- de barras y precio.
INSERT INTO products (
    internal_code, name, active_ingredient_id, concentration, pharmaceutical_form,
    category_id, laboratory_id, brand, description, base_unit_id,
    min_stock_base, max_stock_base, requires_prescription, is_fractionable,
    controls_lot, controls_expiration, status
) VALUES (
    'PRD-' || LPAD(nextval('product_internal_code_seq')::text, 6, '0'),
    'Ibuprofeno 400 mg',
    (SELECT id FROM active_ingredients WHERE name = 'Ibuprofeno'),
    '400 mg',
    'Tableta',
    (SELECT id FROM product_categories WHERE name = 'Antiinflamatorios'),
    (SELECT id FROM laboratories WHERE name = 'Laboratorio Generico S.A.'),
    'Generico',
    'Producto de ejemplo cargado por la migracion de datos iniciales.',
    (SELECT id FROM units WHERE code = 'PASTILLA'),
    20, 500, FALSE, TRUE, TRUE, TRUE, 'ACTIVE'
);

-- Presentacion base: PASTILLA, factor 1
INSERT INTO product_presentations (product_id, unit_id, conversion_factor, sale_price, purchase_price, is_base_presentation, active)
SELECT p.id, (SELECT id FROM units WHERE code = 'PASTILLA'), 1, 500, 300, TRUE, TRUE
FROM products p WHERE p.name = 'Ibuprofeno 400 mg';

INSERT INTO barcodes (presentation_id, code, is_primary)
SELECT pp.id, '770000000011', TRUE
FROM product_presentations pp
JOIN products p ON p.id = pp.product_id
WHERE p.name = 'Ibuprofeno 400 mg' AND pp.is_base_presentation = TRUE;

-- Presentacion TABLETA (blister de 10 pastillas), factor 10
INSERT INTO product_presentations (product_id, unit_id, conversion_factor, sale_price, purchase_price, is_base_presentation, active)
SELECT p.id, (SELECT id FROM units WHERE code = 'TABLETA'), 10, 4500, 2800, FALSE, TRUE
FROM products p WHERE p.name = 'Ibuprofeno 400 mg';

INSERT INTO barcodes (presentation_id, code, is_primary)
SELECT pp.id, '770000000028', TRUE
FROM product_presentations pp
JOIN products p ON p.id = pp.product_id
WHERE p.name = 'Ibuprofeno 400 mg' AND pp.conversion_factor = 10;

-- Presentacion CAJA (10 blisteres = 100 pastillas), factor 100
INSERT INTO product_presentations (product_id, unit_id, conversion_factor, sale_price, purchase_price, is_base_presentation, active)
SELECT p.id, (SELECT id FROM units WHERE code = 'CAJA'), 100, 40000, 26000, FALSE, TRUE
FROM products p WHERE p.name = 'Ibuprofeno 400 mg';

INSERT INTO barcodes (presentation_id, code, is_primary)
SELECT pp.id, '770000000035', TRUE
FROM product_presentations pp
JOIN products p ON p.id = pp.product_id
WHERE p.name = 'Ibuprofeno 400 mg' AND pp.conversion_factor = 100;
