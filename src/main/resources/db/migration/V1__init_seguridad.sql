CREATE TABLE roles (
                       id_rol      SERIAL PRIMARY KEY,
                       nombre      VARCHAR(50) NOT NULL UNIQUE,
                       descripcion VARCHAR(200)
);

CREATE TABLE usuario (
                         id                  SERIAL PRIMARY KEY,
                         firebase_uuid       VARCHAR(128) NOT NULL UNIQUE,
                         tipo_usuario        VARCHAR(20)  NOT NULL CHECK (tipo_usuario IN ('EMPLEADO', 'CLIENTE')),
                         id_rol              INT REFERENCES roles(id_rol),
                         nombre              VARCHAR(100) NOT NULL,
                         apellidos           VARCHAR(100),
                         documento_identidad VARCHAR(20)  UNIQUE,
                         email               VARCHAR(150) NOT NULL UNIQUE,
                         telefono            VARCHAR(20),
                         activo              BOOLEAN      NOT NULL DEFAULT TRUE,
                         created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE funcion (
                         id_funcion    SERIAL PRIMARY KEY,
                         nombre_codigo VARCHAR(50)  NOT NULL UNIQUE,
                         descripcion   VARCHAR(200)
);

CREATE TABLE rol_funcion (
                             id_rol     INT NOT NULL REFERENCES roles(id_rol),
                             id_funcion INT NOT NULL REFERENCES funcion(id_funcion),
                             PRIMARY KEY (id_rol, id_funcion)
);

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
  AND f.nombre_codigo IN ('PROY_VER','USER_GESTIONAR','DOCS_VER','DOCS_SUBIR','PAGOS_VER','CONTRATO_VER');

-- Permisos de LEGAL
INSERT INTO rol_funcion (id_rol, id_funcion)
SELECT r.id_rol, f.id_funcion FROM roles r, funcion f
WHERE r.nombre = 'LEGAL'
  AND f.nombre_codigo IN ('CONTRATO_VER','CONTRATO_EDITAR','DOCS_VER');

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
  AND f.nombre_codigo IN ('PROY_VER','DOCS_VER','PAGOS_VER','OBRA_VER','CONTRATO_VER');

CREATE INDEX idx_usuario_firebase_uuid ON usuario(firebase_uuid);
CREATE INDEX idx_usuario_email         ON usuario(email);
CREATE INDEX idx_rol_funcion_rol       ON rol_funcion(id_rol);