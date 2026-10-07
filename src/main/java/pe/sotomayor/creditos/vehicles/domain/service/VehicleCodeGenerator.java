package pe.sotomayor.creditos.vehicles.domain.service;

import java.util.UUID;

/**
 * Genera identidades publicas en el sistema, sin recibirlas del formulario.
 *
 * @author DarwinG
 */
@FunctionalInterface
public interface VehicleCodeGenerator {
    UUID generate();
}
