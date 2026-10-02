-- Lista estandar de categorias de producto para una drogueria/farmacia,
-- acordada con el cliente (ver conversacion de soporte). Analgesicos y
-- Antiinflamatorios ya existian como datos de prueba (V4) y no se repiten
-- aqui -- product_categories.name tiene restriccion de unicidad logica
-- (ProductCategoryService.create valida existsByNameIgnoreCase).
INSERT INTO product_categories (name, description) VALUES
    ('Antibioticos', 'Medicamentos para tratar infecciones bacterianas'),
    ('Antigripales y resfriado', 'Medicamentos para sintomas de gripa y resfriado comun'),
    ('Antialergicos', 'Medicamentos para el manejo de alergias'),
    ('Antiacidos y digestivos', 'Medicamentos para acidez, reflujo y malestar digestivo'),
    ('Antidiarreicos y laxantes', 'Medicamentos para diarrea y estrenimiento'),
    ('Vitaminas y suplementos', 'Vitaminas, minerales y suplementos nutricionales'),
    ('Dermatologicos', 'Productos para el cuidado y tratamiento de la piel'),
    ('Oftalmicos', 'Gotas y productos para el cuidado de los ojos'),
    ('Oticos', 'Gotas y productos para el cuidado de los oidos'),
    ('Cuidado bucal', 'Productos de higiene y cuidado bucal'),
    ('Cuidado personal e higiene', 'Productos de higiene y cuidado personal'),
    ('Cosmeticos', 'Productos cosmeticos y de belleza'),
    ('Cuidado del bebe e infantil', 'Productos para el cuidado de bebes y ninos'),
    ('Anticonceptivos y planificacion familiar', 'Metodos anticonceptivos y productos de planificacion familiar'),
    ('Dispositivos medicos e insumos', 'Jeringas, gasas, curitas, guantes y otros insumos medicos'),
    ('Ortopedia y soportes', 'Soportes, fajas y productos de ortopedia'),
    ('Cardiovasculares', 'Medicamentos para el manejo de condiciones cardiovasculares'),
    ('Diabetes', 'Glucometros, insulina, tiras reactivas y productos para diabetes'),
    ('Antimicoticos', 'Medicamentos para infecciones por hongos'),
    ('Antiparasitarios', 'Medicamentos para el tratamiento de parasitos'),
    ('Hormonales', 'Medicamentos de tipo hormonal'),
    ('Primeros auxilios', 'Productos e insumos de primeros auxilios');
