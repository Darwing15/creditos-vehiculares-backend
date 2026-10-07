package pe.sotomayor.creditos.customers.application.usecase;

import pe.sotomayor.creditos.customers.application.dto.response.CustomerOptionResponse;

import java.util.List;

/**
 * Caso de uso para listar clientes elegibles en la sucursal actual.
 *
 * @author DarwinG
 */
public interface ListCustomersUseCase {
    /**
     * @return clientes activos de la sucursal autenticada.
     */
    List<CustomerOptionResponse> listActive();
}
