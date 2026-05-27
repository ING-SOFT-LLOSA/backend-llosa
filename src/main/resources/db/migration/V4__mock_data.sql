-- Mock data for project-domain tables (excludes user/security tables)

-- Proyecto
INSERT INTO proyecto (uuid_proyecto, nombre, descripcion, precertificacion_edge_leed, link_recorrido_virtual, departamento, distrito, direccion, fecha_inicio, fecha_fin, created_at)
VALUES
    ('a1b2c3d4-0001-4000-8000-000000000001', 'Edificio Multifamiliar Los Olivos',
     'Proyecto residencial de 20 departamentos con áreas comunes y estacionamientos.',
     FALSE, '', 'Lima', 'San Borja', 'Av. Primavera 1234', '2024-01-15', '2025-06-30', NOW()),
    ('a1b2c3d4-0001-4000-8000-000000000002', 'Condominio El Bosque',
     'Conjunto habitacional con áreas verdes y piscina.',
     TRUE, 'https://tour.example.com/bosque', 'Lima', 'Surco', 'Calle Los Pinos 567', '2024-03-01', '2025-12-15', NOW());

-- Torres
INSERT INTO torre (id_torre, nombre, uuid_proyecto)
VALUES
    (1, 'Torre A', 'a1b2c3d4-0001-4000-8000-000000000001'),
    (2, 'Torre B', 'a1b2c3d4-0001-4000-8000-000000000001'),
    (3, 'Torre Única', 'a1b2c3d4-0001-4000-8000-000000000002');

-- Pisos
INSERT INTO piso (id_piso, nro_piso, id_torre) VALUES
    (1,  1, 1), (2,  2, 1), (3,  3, 1), (4,  4, 1), (5,  5, 1),
    (6,  1, 2), (7,  2, 2), (8,  3, 2), (9,  4, 2), (10, 5, 2),
    (11, 1, 3), (12, 2, 3), (13, 3, 3);

-- Activos (departamentos, cocheras, depósitos)
INSERT INTO activo (uuid_activo, nro, tipo, area_m2, estado_comercial, precio, descripcion, id_piso, created_at, updated_at)
VALUES
    -- Torre A - Piso 1
    ('a1b2c3d4-0002-4000-8000-000000000001', 'A-101', 'DEPARTAMENTO', 85.50, 'DISPONIBLE',  350000.00, 'Departamento de 3 dormitorios', 1, NOW(), NOW()),
    ('a1b2c3d4-0002-4000-8000-000000000002', 'A-102', 'DEPARTAMENTO', 72.00, 'SEPARADO',    280000.00, 'Departamento de 2 dormitorios', 1, NOW(), NOW()),
    ('a1b2c3d4-0002-4000-8000-000000000003', 'A-103', 'DEPARTAMENTO', 90.00, 'VENDIDO',     380000.00, 'Departamento de 3 dormitorios', 1, NOW(), NOW()),
    -- Torre A - Piso 2
    ('a1b2c3d4-0002-4000-8000-000000000004', 'A-201', 'DEPARTAMENTO', 85.50, 'DISPONIBLE',  360000.00, 'Departamento de 3 dormitorios', 2, NOW(), NOW()),
    ('a1b2c3d4-0002-4000-8000-000000000005', 'A-202', 'DEPARTAMENTO', 72.00, 'DISPONIBLE',  290000.00, 'Departamento de 2 dormitorios', 2, NOW(), NOW()),
    -- Torre A - Piso 3
    ('a1b2c3d4-0002-4000-8000-000000000006', 'A-301', 'DEPARTAMENTO', 100.00, 'DISPONIBLE', 420000.00, 'Departamento de 4 dormitorios', 3, NOW(), NOW()),
    -- Torre B - Piso 1
    ('a1b2c3d4-0002-4000-8000-000000000007', 'B-101', 'DEPARTAMENTO', 80.00, 'DISPONIBLE',  320000.00, 'Departamento de 3 dormitorios', 6, NOW(), NOW()),
    ('a1b2c3d4-0002-4000-8000-000000000008', 'B-102', 'DEPARTAMENTO', 65.00, 'SEPARADO',    250000.00, 'Departamento de 2 dormitorios', 6, NOW(), NOW()),
    -- Torre B - Piso 2
    ('a1b2c3d4-0002-4000-8000-000000000009', 'B-201', 'DEPARTAMENTO', 80.00, 'VENDIDO',     330000.00, 'Departamento de 3 dormitorios', 7, NOW(), NOW()),
    ('a1b2c3d4-0002-4000-8000-000000000010', 'B-202', 'DEPARTAMENTO', 65.00, 'DISPONIBLE',  255000.00, 'Departamento de 2 dormitorios', 7, NOW(), NOW()),
    -- Torre Única - Pisos
    ('a1b2c3d4-0002-4000-8000-000000000011', 'U-101', 'DEPARTAMENTO', 95.00, 'DISPONIBLE',  400000.00, 'Departamento de 3 dormitorios', 11, NOW(), NOW()),
    ('a1b2c3d4-0002-4000-8000-000000000012', 'U-102', 'DEPARTAMENTO', 70.00, 'DISPONIBLE',  300000.00, 'Departamento de 2 dormitorios', 11, NOW(), NOW()),
    ('a1b2c3d4-0002-4000-8000-000000000013', 'U-201', 'DEPARTAMENTO', 95.00, 'SEPARADO',    410000.00, 'Departamento de 3 dormitorios', 12, NOW(), NOW()),
    ('a1b2c3d4-0002-4000-8000-000000000014', 'U-301', 'DEPARTAMENTO', 110.00, 'DISPONIBLE',  450000.00, 'Departamento dúplex',        13, NOW(), NOW()),
    -- Cocheras
    ('a1b2c3d4-0002-4000-8000-000000000015', 'CH-A01', 'COCHERA', 12.50, 'DISPONIBLE', 15000.00, 'Cochera sótano Torre A', 1, NOW(), NOW()),
    ('a1b2c3d4-0002-4000-8000-000000000016', 'CH-A02', 'COCHERA', 12.50, 'VENDIDO',    15000.00, 'Cochera sótano Torre A', 1, NOW(), NOW()),
    ('a1b2c3d4-0002-4000-8000-000000000017', 'CH-B01', 'COCHERA', 12.50, 'DISPONIBLE', 15000.00, 'Cochera sótano Torre B', 6, NOW(), NOW()),
    -- Depósitos
    ('a1b2c3d4-0002-4000-8000-000000000018', 'DP-A01', 'DEPOSITO', 5.00, 'DISPONIBLE', 5000.00, 'Depósito Torre A', 1, NOW(), NOW()),
    ('a1b2c3d4-0002-4000-8000-000000000019', 'DP-B01', 'DEPOSITO', 5.00, 'DISPONIBLE', 5000.00, 'Depósito Torre B', 6, NOW(), NOW());

-- Etapas
INSERT INTO etapa (id_etapa, nombre, orden, descripcion, estado, uuid_proyecto)
VALUES
    (1, 'Obra Gruesa',       1, 'Cimentación, estructura y techos',                         'COMPLETADO',  'a1b2c3d4-0001-4000-8000-000000000001'),
    (2, 'Acabados',          2, 'Instalaciones, revestimientos y pintura',                   'EN_PROGRESO', 'a1b2c3d4-0001-4000-8000-000000000001'),
    (3, 'Entrega',           3, 'Trámites finales y entrega de unidades',                    'PENDIENTE',   'a1b2c3d4-0001-4000-8000-000000000001'),
    (4, 'Cimentación',       1, 'Excavación, vaciado de concreto y zapatas',                 'COMPLETADO',  'a1b2c3d4-0001-4000-8000-000000000002'),
    (5, 'Estructura',        2, 'Columnas, vigas y losas',                                   'EN_PROGRESO', 'a1b2c3d4-0001-4000-8000-000000000002'),
    (6, 'Acabados',          3, 'Acabados interiores y exteriores',                          'PENDIENTE',   'a1b2c3d4-0001-4000-8000-000000000002'),
    (7, 'Áreas Comunes',     4, 'Piscina, jardines y sala de usos múltiples',                'PENDIENTE',   'a1b2c3d4-0001-4000-8000-000000000002');

-- Hitos
INSERT INTO hito (uuid_hito, orden, tipo, titulo, estado, fecha_completado, id_etapa)
VALUES
    -- Obra Gruesa (etapa 1)
    ('a1b2c3d4-0003-4000-8000-000000000001', 1, 'OBRA',        'Cimentación',             'COMPLETADO', '2024-03-15', 1),
    ('a1b2c3d4-0003-4000-8000-000000000002', 2, 'OBRA',        'Estructura de concreto',  'COMPLETADO', '2024-06-20', 1),
    ('a1b2c3d4-0003-4000-8000-000000000003', 3, 'OBRA',        'Techos y losas',          'COMPLETADO', '2024-08-10', 1),
    -- Acabados (etapa 2)
    ('a1b2c3d4-0003-4000-8000-000000000004', 1, 'OBRA',        'Instalaciones eléctricas','EN_PROGRESO', NULL, 2),
    ('a1b2c3d4-0003-4000-8000-000000000005', 2, 'OBRA',        'Instalaciones sanitarias', 'EN_PROGRESO', NULL, 2),
    ('a1b2c3d4-0003-4000-8000-000000000006', 3, 'OBRA',        'Revestimientos',          'PENDIENTE',   NULL, 2),
    -- Entrega (etapa 3)
    ('a1b2c3d4-0003-4000-8000-000000000007', 1, 'SANEAMIENTO', 'Independización',         'PENDIENTE',   NULL, 3),
    ('a1b2c3d4-0003-4000-8000-000000000008', 2, 'SANEAMIENTO', 'Declaratoria de fábrica', 'PENDIENTE',   NULL, 3),
    -- Cimentación (etapa 4)
    ('a1b2c3d4-0003-4000-8000-000000000009', 1, 'OBRA',        'Excavación masiva',       'COMPLETADO', '2024-05-01', 4),
    ('a1b2c3d4-0003-4000-8000-000000000010', 2, 'OBRA',        'Zapatas y cimientos',     'COMPLETADO', '2024-07-15', 4),
    -- Estructura (etapa 5)
    ('a1b2c3d4-0003-4000-8000-000000000011', 1, 'OBRA',        'Columnas y placas',       'EN_PROGRESO', NULL, 5),
    ('a1b2c3d4-0003-4000-8000-000000000012', 2, 'OBRA',        'Vigas y losas',           'PENDIENTE',   NULL, 5);

-- Hito_unidad (algunos hitos completados en unidades vendidas/separadas)
INSERT INTO hito_unidad (uuid_hito_unidad, estado, fecha_completado, created_at, updated_at, observaciones, uuid_activo, uuid_hito)
VALUES
    -- Unidad A-103 (vendida) - todos los hitos de obra gruesa completados
    ('a1b2c3d4-0004-4000-8000-000000000001', 'COMPLETADO', '2024-03-15', NOW(), NOW(), 'Sin observaciones',
     'a1b2c3d4-0002-4000-8000-000000000003', 'a1b2c3d4-0003-4000-8000-000000000001'),
    ('a1b2c3d4-0004-4000-8000-000000000002', 'COMPLETADO', '2024-06-20', NOW(), NOW(), 'Todo conforme',
     'a1b2c3d4-0002-4000-8000-000000000003', 'a1b2c3d4-0003-4000-8000-000000000002'),
    ('a1b2c3d4-0004-4000-8000-000000000003', 'COMPLETADO', '2024-08-10', NOW(), NOW(), NULL,
     'a1b2c3d4-0002-4000-8000-000000000003', 'a1b2c3d4-0003-4000-8000-000000000003'),
    -- Unidad A-102 (separada) - obra gruesa completada, acabados en progreso
    ('a1b2c3d4-0004-4000-8000-000000000004', 'COMPLETADO', '2024-03-15', NOW(), NOW(), NULL,
     'a1b2c3d4-0002-4000-8000-000000000002', 'a1b2c3d4-0003-4000-8000-000000000001'),
    ('a1b2c3d4-0004-4000-8000-000000000005', 'COMPLETADO', '2024-06-20', NOW(), NOW(), NULL,
     'a1b2c3d4-0002-4000-8000-000000000002', 'a1b2c3d4-0003-4000-8000-000000000002'),
    ('a1b2c3d4-0004-4000-8000-000000000006', 'COMPLETADO', '2024-08-10', NOW(), NOW(), NULL,
     'a1b2c3d4-0002-4000-8000-000000000002', 'a1b2c3d4-0003-4000-8000-000000000003'),
    ('a1b2c3d4-0004-4000-8000-000000000007', 'EN_PROGRESO', NULL, NOW(), NOW(), 'Instalaciones en proceso',
     'a1b2c3d4-0002-4000-8000-000000000002', 'a1b2c3d4-0003-4000-8000-000000000004'),
    -- Unidad B-201 (vendida) - obra gruesa completada
    ('a1b2c3d4-0004-4000-8000-000000000008', 'COMPLETADO', '2024-03-20', NOW(), NOW(), NULL,
     'a1b2c3d4-0002-4000-8000-000000000009', 'a1b2c3d4-0003-4000-8000-000000000001'),
    ('a1b2c3d4-0004-4000-8000-000000000009', 'COMPLETADO', '2024-07-01', NOW(), NOW(), NULL,
     'a1b2c3d4-0002-4000-8000-000000000009', 'a1b2c3d4-0003-4000-8000-000000000002');
