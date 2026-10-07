package pe.sotomayor.creditos.customers.application.usecase;

import pe.sotomayor.creditos.customers.application.dto.request.CreateCustomerRequest;
import pe.sotomayor.creditos.customers.application.dto.response.CustomerResponse;

/**
 * Contrato del caso de uso create de Customer.
 *
 * @author DarwinG
 */
public interface CreateCustomerUseCase {
    CustomerResponse create(CreateCustomerRequest request);
}
