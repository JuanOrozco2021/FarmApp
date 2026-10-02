-- Roles iniciales
INSERT INTO roles (code, name, description) VALUES
    ('ADMIN',        'Administrador',            'Acceso total al sistema'),
    ('FARMACEUTICO', 'Farmaceutico / Administrador de farmacia', 'Gestiona catalogo, inventario, compras y precios'),
    ('CAJERO',       'Cajero',                    'Opera el punto de venta'),
    ('INYECTOLOGIA', 'Inyectologia',              'Registra el servicio de inyectologia');

-- Permisos iniciales (se amplian en fases posteriores segun cada modulo)
INSERT INTO permissions (code, description) VALUES
    ('USER_MANAGE',        'Crear, editar y desactivar usuarios'),
    ('ROLE_MANAGE',        'Administrar roles y permisos'),
    ('PRODUCT_MANAGE',     'Crear y editar productos, presentaciones y precios'),
    ('PURCHASE_MANAGE',    'Registrar y confirmar compras'),
    ('INVENTORY_ADJUST',   'Realizar ajustes manuales de inventario'),
    ('SALE_REGISTER',      'Registrar ventas en el punto de venta'),
    ('SALE_APPLY_DISCOUNT','Aplicar descuentos en una venta'),
    ('INJECTION_REGISTER', 'Registrar servicios de inyectologia'),
    ('AUDIT_VIEW',         'Consultar el historial de auditoria'),
    ('SETTINGS_MANAGE',    'Modificar la configuracion del negocio');

-- ADMIN: todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'ADMIN'), id FROM permissions;

-- FARMACEUTICO
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'FARMACEUTICO'), id
FROM permissions
WHERE code IN ('PRODUCT_MANAGE','PURCHASE_MANAGE','INVENTORY_ADJUST','SALE_REGISTER','SALE_APPLY_DISCOUNT','SETTINGS_MANAGE');

-- CAJERO: puede vender, pero NO aplicar descuento (decision confirmada)
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'CAJERO'), id
FROM permissions
WHERE code IN ('SALE_REGISTER');

-- INYECTOLOGIA
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'INYECTOLOGIA'), id
FROM permissions
WHERE code IN ('INJECTION_REGISTER');
