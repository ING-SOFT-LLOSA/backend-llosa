CREATE TABLE documento (
                           uuid_documento      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                           ruta_gcs            VARCHAR(500) NOT NULL,
                           nombre_original     VARCHAR(255) NOT NULL,
                           id_referencia       VARCHAR(36)  NOT NULL,
                           entidad_referencia  VARCHAR(50)  NOT NULL,
                           tipo_documento      VARCHAR(50)  NOT NULL,
                           tipo_mime           VARCHAR(50),
                           acceso_restringido  BOOLEAN      NOT NULL DEFAULT TRUE,
                           subido_por          INT          REFERENCES usuario(id),
                           created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documento_referencia ON documento(id_referencia, entidad_referencia);