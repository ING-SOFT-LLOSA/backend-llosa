CREATE TABLE IF NOT EXISTS proyecto (
    uuid_proyecto              UUID           NOT NULL PRIMARY KEY,
    nombre                     VARCHAR(255)   NOT NULL,
    descripcion                TEXT,
    precertificacion_edge_leed BOOLEAN        DEFAULT FALSE,
    link_recorrido_virtual     VARCHAR(255)   DEFAULT '',
    departamento               VARCHAR(255),
    distrito                   VARCHAR(255),
    direccion                  VARCHAR(255),
    fecha_inicio               DATE,
    fecha_fin                  DATE,
    created_at                 TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS torre (
    id_torre      BIGSERIAL    NOT NULL PRIMARY KEY,
    nombre        VARCHAR(255) NOT NULL,
    uuid_proyecto UUID         NOT NULL REFERENCES proyecto(uuid_proyecto)
);

CREATE TABLE IF NOT EXISTS piso (
    id_piso   BIGSERIAL  NOT NULL PRIMARY KEY,
    nro_piso  INTEGER    NOT NULL,
    id_torre  BIGINT     NOT NULL REFERENCES torre(id_torre)
);

CREATE TABLE IF NOT EXISTS activo (
    uuid_activo      UUID           NOT NULL PRIMARY KEY,
    nro              VARCHAR(255)   NOT NULL,
    tipo             VARCHAR(255)   NOT NULL,
    area_m2          DECIMAL(38,2)  NOT NULL DEFAULT 0.00,
    estado_comercial VARCHAR(255)   NOT NULL,
    precio           DECIMAL(38,2)  DEFAULT 0.00,
    descripcion      VARCHAR(255)   DEFAULT 'No existe descripcion todavia',
    created_at       TIMESTAMP(6),
    updated_at       TIMESTAMP(6),
    id_piso          BIGINT         NOT NULL REFERENCES piso(id_piso)
);

CREATE TABLE IF NOT EXISTS etapa (
    id_etapa      BIGSERIAL     NOT NULL PRIMARY KEY,
    nombre        VARCHAR(255)  NOT NULL,
    orden         INTEGER       NOT NULL,
    descripcion   VARCHAR(200)  DEFAULT 'Descripcion aún no definida',
    estado        VARCHAR(255)  NOT NULL,
    uuid_proyecto UUID          NOT NULL REFERENCES proyecto(uuid_proyecto)
);

CREATE TABLE IF NOT EXISTS hito (
    uuid_hito        UUID          NOT NULL PRIMARY KEY,
    orden            INTEGER       NOT NULL,
    tipo             VARCHAR(255)  NOT NULL,
    titulo           VARCHAR(255),
    estado           VARCHAR(255)  NOT NULL,
    fecha_completado DATE,
    id_etapa         BIGINT        NOT NULL REFERENCES etapa(id_etapa)
);

CREATE TABLE IF NOT EXISTS hito_unidad (
    uuid_hito_unidad UUID          NOT NULL PRIMARY KEY,
    estado           VARCHAR(255)  NOT NULL,
    fecha_completado DATE,
    created_at       TIMESTAMP(6),
    updated_at       TIMESTAMP(6),
    observaciones    TEXT,
    uuid_activo      UUID          NOT NULL REFERENCES activo(uuid_activo),
    uuid_hito        UUID          NOT NULL REFERENCES hito(uuid_hito),
    UNIQUE (uuid_activo, uuid_hito)
);

CREATE TABLE IF NOT EXISTS usuario_activo (
    uuid_usuario_activo  UUID          NOT NULL PRIMARY KEY,
    tipo_financiamiento  VARCHAR(255),
    fase_comercial       VARCHAR(255),
    estado_tramite_legal VARCHAR(255),
    fecha_adquisicion    TIMESTAMP(6),
    created_at           TIMESTAMP(6),
    updated_at           TIMESTAMP(6),
    id_usuario           INT           NOT NULL REFERENCES usuario(id),
    uuid_activo          UUID          NOT NULL REFERENCES activo(uuid_activo),
    CONSTRAINT uk_usuario_activo UNIQUE (id_usuario, uuid_activo)
);
