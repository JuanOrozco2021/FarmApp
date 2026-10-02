-- INJECTION_REGISTER ya existia desde V2 (ADMIN lo tiene por el wildcard
-- de ese momento, INYECTOLOGIA lo tiene explicito). FARMACEUTICO no lo
-- tenia porque en la Fase 1 el modulo todavia no existia: en una farmacia
-- pequena el farmaceutico tambien puede aplicar inyecciones, asi que se
-- le otorga aqui.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'FARMACEUTICO'), id
FROM permissions
WHERE code = 'INJECTION_REGISTER';

-- Anular un registro de inyectologia revierte un cobro ya hecho: se le da
-- su propio permiso separado de INJECTION_REGISTER (igual criterio que
-- SALE_CANCEL vs SALE_REGISTER en la Fase 6) — quien aplica la inyeccion
-- no deberia poder anular su propio cobro.
INSERT INTO permissions (code, description) VALUES
    ('INJECTION_CANCEL', 'Anular un registro de inyectologia ya cobrado');

-- Leccion de siempre: el "todos los permisos" de ADMIN en V2 no recoge
-- retroactivamente este permiso nuevo.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'ADMIN'), id
FROM permissions
WHERE code = 'INJECTION_CANCEL';

INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE code = 'FARMACEUTICO'), id
FROM permissions
WHERE code = 'INJECTION_CANCEL';
