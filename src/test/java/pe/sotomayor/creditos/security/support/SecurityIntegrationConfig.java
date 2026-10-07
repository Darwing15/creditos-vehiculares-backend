package pe.sotomayor.creditos.security.support;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.containers.PostgreSQLContainer;
import pe.sotomayor.creditos.shared.config.JpaAuditConfig;
import pe.sotomayor.creditos.shared.config.TimeConfig;
import pe.sotomayor.creditos.shared.exception.GlobalExceptionHandler;

@Configuration(proxyBeanMethods = false)
@Profile("security-integration")
@EnableAutoConfiguration
@EntityScan("pe.sotomayor.creditos.security.domain.model.entity")
@EnableJpaRepositories("pe.sotomayor.creditos.security.domain.repository")
@ComponentScan(basePackages = {
        "pe.sotomayor.creditos.security.application",
        "pe.sotomayor.creditos.security.infrastructure.security",
        "pe.sotomayor.creditos.security.presentation"})
@Import({JpaAuditConfig.class, TimeConfig.class, GlobalExceptionHandler.class})
public class SecurityIntegrationConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> securityPostgres() {
        return new PostgreSQLContainer<>("postgres:17-alpine");
    }
}
