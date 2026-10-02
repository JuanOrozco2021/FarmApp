INSERT INTO business_settings (id, pharmacy_name, expiration_alert_days) VALUES (1, 'Boticario', 60);

-- Saldo inicial de ejemplo para el producto demo (Ibuprofeno 400 mg), repartido
-- en dos lotes con vencimientos distintos para poder probar FEFO de una vez
-- (Lote A vence primero -> debe salir primero al vender/consumir).
DO $$
DECLARE
    v_product_id BIGINT;
    v_lot_a_id BIGINT;
    v_lot_b_id BIGINT;
BEGIN
    SELECT id INTO v_product_id FROM products WHERE name = 'Ibuprofeno 400 mg';

    IF v_product_id IS NOT NULL THEN
        INSERT INTO inventory_lots (product_id, lot_code, expiration_date, quantity_base)
        VALUES (v_product_id, 'LOTE-DEMO-A', (now() + interval '6 months')::date, 150)
        RETURNING id INTO v_lot_a_id;

        INSERT INTO inventory_lots (product_id, lot_code, expiration_date, quantity_base)
        VALUES (v_product_id, 'LOTE-DEMO-B', (now() + interval '18 months')::date, 150)
        RETURNING id INTO v_lot_b_id;

        INSERT INTO inventory_stock (product_id, quantity_base, version)
        VALUES (v_product_id, 300, 0);

        INSERT INTO inventory_movements (product_id, lot_id, movement_type, quantity_base, balance_after, reference_type, notes)
        VALUES
            (v_product_id, v_lot_a_id, 'AJUSTE_POSITIVO', 150, 150, 'INITIAL_BALANCE', 'Saldo inicial de migracion de datos (lote A)'),
            (v_product_id, v_lot_b_id, 'AJUSTE_POSITIVO', 150, 300, 'INITIAL_BALANCE', 'Saldo inicial de migracion de datos (lote B)');
    END IF;
END $$;
