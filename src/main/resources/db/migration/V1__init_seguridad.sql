CREATE TABLE roles (
                       id_rol  SERIAL PRIMARY KEY,
                       nombre  VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE usuario (
                         id                  SERIAL PRIMARY KEY,
                         firebase_uuid       VARCHAR(128) NOT NULL UNIQUE,
                         tipo_usuario        VARCHAR(20)  NOT NULL CHECK (tipo_usuario IN ('EMPLEADO', 'CLIENTE')),
                         id_rol_base         INT REFERENCES roles(id_rol),
                         nombre              VARCHAR(100) NOT NULL,
                         apellidos           VARCHAR(100),
                         documento_identidad VARCHAR(20)  UNIQUE,
                         email               VARCHAR(150) NOT NULL UNIQUE,
                         telefono            VARCHAR(20),
                         estado              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
                         created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE permiso_usuario (
                                 id          SERIAL PRIMARY KEY,
                                 id_usuario  INT         NOT NULL REFERENCES usuario(id),
                                 modulo      VARCHAR(50) NOT NULL,
                                 acceso      BOOLEAN     NOT NULL,
                                 UNIQUE (id_usuario, modulo)
);

INSERT INTO roles (nombre) VALUES
                               ('ADMIN'),
                               ('ASESOR'),
                               ('LEGAL'),
                               ('TECNICO'),
                               ('POSTVENTA');

CREATE INDEX idx_usuario_firebase_uuid ON usuario(firebase_uuid);
CREATE INDEX idx_usuario_email         ON usuario(email);
CREATE INDEX idx_permiso_usuario_id    ON permiso_usuario(id_usuario);