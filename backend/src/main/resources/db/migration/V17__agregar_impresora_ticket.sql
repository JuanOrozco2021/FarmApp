-- Nombre de la impresora (tal como aparece en Windows) que se debe usar
-- para imprimir el ticket de venta via QZ Tray en modo ESC/POS crudo.
-- Opcional: si queda vacio, el sistema usa el dialogo de impresion normal
-- del navegador como respaldo.
ALTER TABLE business_settings
    ADD COLUMN ticket_printer_name VARCHAR(100);
