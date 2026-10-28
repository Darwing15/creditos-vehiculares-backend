package pe.sotomayor.creditos.customers.domain.service;

import java.time.Instant;

/**
 * Reserva presupuesto antes del intento externo; los errores tambien consumen la reserva local.
 *
 * @author DarwinG
 */
public interface IdentityLookupBudget {
    /**
     * Rechaza solicitudes que excedan frecuencia o presupuesto; no aplica a aciertos de cache.
     */
    void reserve(Long actorId, Instant now);
}
