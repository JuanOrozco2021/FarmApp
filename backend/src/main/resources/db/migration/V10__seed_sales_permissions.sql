-- Anular una venta revierte inventario (movimientos DEVOLUCION_VENTA), asi
-- que se trata como una operacion distinta y mas sensible que registrar
-- una venta: se le da su propio permiso en vez de reutilizar
-- SALE_APPLY_DISCOUNT (que significa otra cosa) o SALE_REGISTER (que el
-- cajero si tiene, y anular no deberia quedar en sus manos).
INSERT INTO permissions (code, description) VALUES
    ('SALE_CANCEL', 'Anular una venta ya registrada (revierte el inventario)');

-- V2 le dio a ADMIN "todos los permisos" con un SELECT sobre la tabla
-- permissions en ESE momento: no recoge retroactivamente filas nuevas.
-- Cualquier permiso agregado en una fase posterior debe otorgarse aqui
-- explicitamente a ADMIN (y a los demas roles que corresponda).
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'ADMIN'), id
FROM permissions
WHERE code = 'SALE_CANCEL';

INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'FARMACEUTICO'), id
FROM permissions
WHERE code = 'SALE_CANCEL';
