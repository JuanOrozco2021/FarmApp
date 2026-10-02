-- Igual leccion que V10 (SALE_CANCEL): el "todos los permisos" de ADMIN en
-- V2 ya se ejecuto y no recoge retroactivamente filas nuevas. Cualquier
-- permiso agregado en una fase posterior se otorga aqui explicitamente.
INSERT INTO permissions (code, description) VALUES
    ('CUSTOMER_MANAGE', 'Crear y editar clientes');

-- Bajo riesgo (es solo dato de contacto, no afecta inventario ni dinero):
-- se le da a los tres roles que atienden mostrador.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'ADMIN'), id
FROM permissions
WHERE code = 'CUSTOMER_MANAGE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'FARMACEUTICO'), id
FROM permissions
WHERE code = 'CUSTOMER_MANAGE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'CAJERO'), id
FROM permissions
WHERE code = 'CUSTOMER_MANAGE';
