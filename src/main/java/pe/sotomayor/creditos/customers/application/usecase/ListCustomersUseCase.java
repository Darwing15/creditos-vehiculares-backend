package pe.sotomayor.creditos.customers.application.usecase;

import pe.sotomayor.creditos.customers.application.dto.response.CustomerOptionResponse;
import pe.sotomayor.creditos.customers.application.dto.response.CustomerPageResponse;

import pe.sotomayor.creditos.shared.exception.ValidationException;

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

    /**
     * Busca clientes activos de la sucursal autenticada.
     *
     * @param query fragmento literal opcional de nombre, documento, telefono o direccion
     * @param page  pagina desde cero, hasta 1 000 000
     * @param size  tamaño de pagina entre 1 y 100
     * @return pagina ordenada por identificador y sus totales
     * @throws ValidationException si la busqueda o paginacion son invalidas
     */
    CustomerPageResponse searchPage(String query, int page, int size);
}
