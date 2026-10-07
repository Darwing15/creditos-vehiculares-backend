package pe.sotomayor.creditos.vehicles.infrastructure.config;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.sotomayor.creditos.vehicles.domain.service.VehicleCodeGenerator;

/**
 * Configura la generacion de identidades publicas.
 *
 * @author DarwinG
 */
@Configuration
public class VehicleDomainConfig {
    /**
     * @return generador de UUID para altas.
     */
    @Bean
    public VehicleCodeGenerator vehicleCodeGenerator() {
        return UUID::randomUUID;
    }
}

