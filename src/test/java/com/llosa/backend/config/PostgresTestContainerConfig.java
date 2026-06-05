package com.llosa.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainerConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        String postgresVersion = System.getenv().getOrDefault("POSTGRES_VERSION", "16");
        String dbName = System.getenv().getOrDefault("DB_NAME", "testdb");
        String dbUser = System.getenv().getOrDefault("DB_USERNAME", "test");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "test");

        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:" + postgresVersion))
                .withDatabaseName(dbName)
                .withUsername(dbUser)
                .withPassword(dbPassword)
                .withReuse(false);
    }
}
