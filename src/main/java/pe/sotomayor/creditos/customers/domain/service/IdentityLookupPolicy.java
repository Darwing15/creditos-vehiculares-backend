package pe.sotomayor.creditos.customers.domain.service;

import java.time.Duration;

/**
 * Politica configurable de consumo y vigencia.
 *
 * @author DarwinG
 */
public interface IdentityLookupPolicy {
    /**
     * @return si se permiten nuevas llamadas externas.
     */
    boolean enabled();

    /**
     * @return vigencia de resultados exitosos persistidos.
     */
    Duration cacheTtl();
}

