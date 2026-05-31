
-- Roles base
INSERT INTO roles (nombre, descripcion) VALUES
                                            ('ADMIN',     'Administrador del sistema'),
                                            ('ASESOR',    'Asesor de ventas'),
                                            ('LEGAL',     'Área legal'),
                                            ('TECNICO',   'Área técnica'),
                                            ('POSTVENTA', 'Área de postventa'),
                                            ('CLIENTE',   'Cliente de la inmobiliaria');

-- Funciones del sistema
INSERT INTO funcion (nombre_codigo, descripcion) VALUES
                                                     ('PROY_VER',       'Ver proyectos'),
                                                     ('PROY_CREAR',     'Crear proyectos'),
                                                     ('PROY_EDITAR',    'Editar proyectos'),
                                                     ('USER_GESTIONAR', 'Gestionar usuarios'),
                                                     ('USER_VER',       'Ver usuarios'),
                                                     ('ROL_GESTIONAR',  'Gestionar roles y funciones'),
                                                     ('DOCS_VER',       'Ver documentos'),
                                                     ('DOCS_SUBIR',     'Subir documentos'),
                                                     ('PAGOS_VER',      'Ver pagos'),
                                                     ('CONTRATO_VER',   'Ver contratos'),
                                                     ('CONTRATO_EDITAR','Editar contratos'),
                                                     ('OBRA_VER',       'Ver avance de obra'),
                                                     ('OBRA_EDITAR',    'Editar avance de obra');

-- Permisos del ADMIN (todo)
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion
FROM roles r, funcion f
WHERE r.nombre = 'ADMIN';

-- Permisos del ASESOR
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion FROM roles r, funcion f
WHERE r.nombre = 'ASESOR'
  AND f.nombre_codigo IN ('PROY_VER','USER_GESTIONAR','USER_VER','DOCS_VER','DOCS_SUBIR','PAGOS_VER','CONTRATO_VER');

-- Permisos de LEGAL
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion FROM roles r, funcion f
WHERE r.nombre = 'LEGAL'
  AND f.nombre_codigo IN ('CONTRATO_VER','CONTRATO_EDITAR','DOCS_VER','USER_VER');

-- Permisos de TECNICO
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion FROM roles r, funcion f
WHERE r.nombre = 'TECNICO'
  AND f.nombre_codigo IN ('OBRA_VER','OBRA_EDITAR','PROY_VER');

-- Permisos de POSTVENTA
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion FROM roles r, funcion f
WHERE r.nombre = 'POSTVENTA'
  AND f.nombre_codigo IN ('PROY_VER','DOCS_VER','PAGOS_VER');

-- Permisos de CLIENTE
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion FROM roles r, funcion f
WHERE r.nombre = 'CLIENTE'
  AND f.nombre_codigo IN ('PROY_VER','DOCS_VER','PAGOS_VER','OBRA_VER','CONTRATO_VER','USER_VER');

CREATE INDEX idx_usuario_firebase_uuid ON usuario(firebase_uuid);
CREATE INDEX idx_usuario_email         ON usuario(email);
CREATE INDEX idx_rol_funcion_rol       ON rol_funcion(id_rol);

-- Usuario superadmin por defecto para desarrollo
-- Firebase project: llosa-edificaciones
INSERT INTO usuario (firebase_uuid, tipo_usuario, id_rol, nombre, apellidos, email, activo,created_at)
VALUES (
           'DmwXY6eVpcOUhf7TnSUKYQvKlIj2',
           'EMPLEADO',
           (SELECT id_rol FROM roles WHERE nombre = 'ADMIN'),
           'Super',
           'Admin',
           'superadmin@llosa.com',
           true,
           CURRENT_TIMESTAMP
       );
-- Firebase project: llosa-edificaciones
INSERT INTO usuario (firebase_uuid, tipo_usuario, id_rol, nombre, apellidos, email, activo,created_at)
VALUES (
    'DqH1tZKj8GU0AcTsev7JmVWt8xj2',
    'CLIENTE',
    (SELECT id_rol FROM roles WHERE nombre = 'CLIENTE'),
    'Jose',
    'Huaman',
    'jose.huaman@utec.edu.pe',
    true,
    CURRENT_TIMESTAMP
    )
