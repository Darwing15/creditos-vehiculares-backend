package pe.sotomayor.creditos.customers.infrastructure.config;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.sotomayor.creditos.customers.domain.service.CustomerCodeGenerator;

/**
 * Configura la generacion de identidades publicas.
 *
 * @author DarwinG
 */
@Configuration
public class CustomerDomainConfig {
    /**
     * @return generador de UUID para altas.
     */
    @Bean
    public CustomerCodeGenerator customerCodeGenerator() {
        return UUID::randomUUID;
    }
}
