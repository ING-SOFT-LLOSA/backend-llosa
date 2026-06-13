package com.llosa.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Provee únicamente el bean {@code gcsBucketName} (String) que
 * {@code DocumentoService} requiere en su constructor.
 *
 * En tests, {@link GcsConfig} (que normalmente lo aporta) está deshabilitado por
 * {@code @Profile("!test")}, y {@link SecurityTestConfiguration} solo mockea el
 * bean {@code Storage}, no el nombre del bucket. Sin este bean, cualquier test
 * que cargue {@code DocumentoService} falla con "No qualifying bean of type
 * java.lang.String".
 *
 * Los tests de la Bóveda Digital (CP25–CP28) usan {@link GcsTestConfig} (emulador
 * real), que ya provee su propio {@code gcsBucketName}; el resto de tests de
 * integración importan este config ligero.
 */
@TestConfiguration(proxyBeanMethods = false)
public class GcsBucketNameTestConfig {

    @Bean
    public String gcsBucketName() {
        return "test-bucket";
    }
}
