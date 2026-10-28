package pe.sotomayor.creditos.customers.application.usecase;

import pe.sotomayor.creditos.customers.application.dto.request.IdentityLookupRequest;
import pe.sotomayor.creditos.customers.application.dto.response.IdentityLookupResponse;

/**
 * Consulta datos de DNI para el alta de clientes, sin registrar al cliente.
 *
 * @author DarwinG
 */
public interface LookupCustomerIdentityUseCase {
    /**
     * @return nombre normalizado y procedencia del resultado.
     */
    IdentityLookupResponse lookup(IdentityLookupRequest request);
}
