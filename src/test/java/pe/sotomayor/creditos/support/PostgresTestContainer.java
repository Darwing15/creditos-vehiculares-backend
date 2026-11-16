package pe.sotomayor.creditos.support;

import org.springframework.boot.test.context.TestConfiguration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import org.springframework.context.annotation.Bean;

import org.testcontainers.containers.PostgreSQLContainer;

/*
 * Registra PostgreSQL como bean para que Spring Boot inyecte la conexion
 * y administre su ciclo de vida. No utiliza DB_URL ni la base de desarrollo.
 *
 * @author DarwinG
 */
@TestConfiguration(proxyBeanMethods = false)
public final class PostgresTestContainer {

    /*
     * @return contenedor aislado para el contexto de pruebas
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17-alpine");
    }

}
