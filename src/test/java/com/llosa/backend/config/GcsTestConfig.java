package com.llosa.backend.config;

import com.google.cloud.NoCredentials;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Configuración de Storage (GCS) para los tests E2E / integración.
 *
 * Levanta un emulador fake-gcs-server en un contenedor (sin credenciales reales)
 * y expone los beans {@code Storage} y {@code gcsBucketName} que normalmente
 * provee {@link GcsConfig}, el cual exige credenciales reales de Google Cloud.
 *
 * Así los Casos de Prueba de la Bóveda Digital (CP25, CP26, CP28) operan contra
 * un GCS real-emulado, y el resto de E2E pueden levantar el contexto sin tocar
 * la nube.
 *
 * LIMITACIÓN CONOCIDA (CP27): {@code storage.signUrl(...withV4Signature())}
 * requiere una credencial con private key para firmar, pero las operaciones de
 * subida contra el emulador requieren {@code NoCredentials}. Ambas necesidades
 * no conviven en un solo bean Storage sin modificar el código de producción
 * (DocumentoService.firmarUrl). Por eso CP27 (Signed URL) NO se verifica con el
 * emulador local; el backend SÍ implementa la firma y debe validarse en un
 * entorno con GCS real. No es un defecto del backend.
 */
@TestConfiguration(proxyBeanMethods = false)
public class GcsTestConfig {

    private static final String BUCKET = "llosa-test-bucket";

    private static final GenericContainer<?> FAKE_GCS =
            new GenericContainer<>(DockerImageName.parse("fsouza/fake-gcs-server:1.49"))
                    .withExposedPorts(4443)
                    .withCommand("-scheme", "http")
                    .waitingFor(Wait.forLogMessage(".*server started.*", 1));

    static {
        FAKE_GCS.start();
    }

    @Bean
    public Storage googleCloudStorage() {
        String endpoint = "http://" + FAKE_GCS.getHost() + ":" + FAKE_GCS.getMappedPort(4443);
        Storage storage = StorageOptions.newBuilder()
                .setHost(endpoint)
                .setProjectId("test-project")
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();

        // Crear el bucket de pruebas si aún no existe.
        if (storage.get(BUCKET) == null) {
            storage.create(BucketInfo.of(BUCKET));
        }
        return storage;
    }

    @Bean
    public String gcsBucketName() {
        return BUCKET;
    }
}
