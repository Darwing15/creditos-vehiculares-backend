package pe.sotomayor.creditos.customers.domain.service;

import java.util.UUID;

/**
 * Genera identidades publicas en el sistema, sin recibirlas del formulario.
 *
 * @author DarwinG
 */
@FunctionalInterface
public interface CustomerCodeGenerator {
    UUID generate();
}
