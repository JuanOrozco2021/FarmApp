-- NIT y direccion del negocio para el encabezado del ticket de venta
-- (regla del encargo: nombre, NIT como subtitulo, direccion debajo).
-- Ambos opcionales: una farmacia puede no tener estos datos cargados
-- todavia y el ticket simplemente no muestra la linea correspondiente.
ALTER TABLE business_settings
    ADD COLUMN nit VARCHAR(30),
    ADD COLUMN address VARCHAR(255);
