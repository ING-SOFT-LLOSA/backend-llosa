-- Hibernate con ddl-auto=create borró todos los datos insertados por
-- V1 y V2 en deploys anteriores. Esta migración recupera todo el seed
-- data sin importar el estado actual de flyway_schema_history.

-- Roles
INSERT INTO roles (nombre, descripcion)
SELECT v.nombre, v.descripcion
FROM (VALUES
    ('ADMIN',     'Administrador del sistema'),
    ('ASESOR',    'Asesor de ventas'),
    ('LEGAL',     'Área legal'),
    ('TECNICO',   'Área técnica'),
    ('POSTVENTA', 'Área de postventa'),
    ('CLIENTE',   'Cliente de la inmobiliaria')
) AS v(nombre, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.nombre = v.nombre);

-- Funciones
INSERT INTO funcion (nombre_codigo, descripcion)
SELECT v.nombre_codigo, v.descripcion
FROM (VALUES
    ('PROY_VER',       'Ver proyectos'),
    ('PROY_CREAR',     'Crear proyectos'),
    ('PROY_EDITAR',    'Editar proyectos'),
    ('USER_GESTIONAR', 'Gestionar usuarios'),
    ('ROL_GESTIONAR',  'Gestionar roles y funciones'),
    ('DOCS_VER',       'Ver documentos'),
    ('DOCS_SUBIR',     'Subir documentos'),
    ('PAGOS_VER',      'Ver pagos'),
    ('CONTRATO_VER',   'Ver contratos'),
    ('CONTRATO_EDITAR','Editar contratos'),
    ('OBRA_VER',       'Ver avance de obra'),
    ('OBRA_EDITAR',    'Editar avance de obra')
) AS v(nombre_codigo, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM funcion f WHERE f.nombre_codigo = v.nombre_codigo);

-- Permisos del ADMIN (todas las funciones)
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion
FROM roles r, funcion f
WHERE r.nombre = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM rol_funcion rf WHERE rf.id_rol = r.id_rol AND rf.id_funcion = f.id_funcion
  );

-- Permisos del ASESOR
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion
FROM roles r, funcion f
WHERE r.nombre = 'ASESOR'
  AND f.nombre_codigo IN ('PROY_VER','USER_GESTIONAR','DOCS_VER','DOCS_SUBIR','PAGOS_VER','CONTRATO_VER')
  AND NOT EXISTS (
    SELECT 1 FROM rol_funcion rf WHERE rf.id_rol = r.id_rol AND rf.id_funcion = f.id_funcion
  );

-- Permisos de LEGAL
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion
FROM roles r, funcion f
WHERE r.nombre = 'LEGAL'
  AND f.nombre_codigo IN ('CONTRATO_VER','CONTRATO_EDITAR','DOCS_VER')
  AND NOT EXISTS (
    SELECT 1 FROM rol_funcion rf WHERE rf.id_rol = r.id_rol AND rf.id_funcion = f.id_funcion
  );

-- Permisos de TECNICO
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion
FROM roles r, funcion f
WHERE r.nombre = 'TECNICO'
  AND f.nombre_codigo IN ('OBRA_VER','OBRA_EDITAR','PROY_VER')
  AND NOT EXISTS (
    SELECT 1 FROM rol_funcion rf WHERE rf.id_rol = r.id_rol AND rf.id_funcion = f.id_funcion
  );

-- Permisos de POSTVENTA
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion
FROM roles r, funcion f
WHERE r.nombre = 'POSTVENTA'
  AND f.nombre_codigo IN ('PROY_VER','DOCS_VER','PAGOS_VER')
  AND NOT EXISTS (
    SELECT 1 FROM rol_funcion rf WHERE rf.id_rol = r.id_rol AND rf.id_funcion = f.id_funcion
  );

-- Permisos de CLIENTE
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion
FROM roles r, funcion f
WHERE r.nombre = 'CLIENTE'
  AND f.nombre_codigo IN ('PROY_VER','DOCS_VER','PAGOS_VER','OBRA_VER','CONTRATO_VER')
  AND NOT EXISTS (
    SELECT 1 FROM rol_funcion rf WHERE rf.id_rol = r.id_rol AND rf.id_funcion = f.id_funcion
  );

-- Usuario admin
INSERT INTO usuario (firebase_uuid, tipo_usuario, id_rol, nombre, apellidos, email, activo)
SELECT 'AMLtHuGEe9dLVca353dLX3O6YKy1', 'EMPLEADO', r.id_rol, 'Super', 'Admin', 'superadmin@llosa.com', true
FROM roles r
WHERE r.nombre = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM usuario u
    WHERE u.firebase_uuid = 'AMLtHuGEe9dLVca353dLX3O6YKy1'
       OR u.email = 'superadmin@gmail.com'
  )
LIMIT 1;