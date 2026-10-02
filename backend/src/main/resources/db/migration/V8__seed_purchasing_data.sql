-- Proveedor de ejemplo para poder registrar una compra de prueba sin dar
-- de alta nada primero (mismo criterio usado con el producto demo de la
-- Fase 3 y los lotes demo de la Fase 4).
INSERT INTO suppliers (name, tax_id, phone, email, address, active)
VALUES ('Distribuidora Central S.A.S.', '900123456-7', '+57 601 555 0100',
        'ventas@distribuidoracentral.com', 'Cra 45 #12-30, Bogota', true);
