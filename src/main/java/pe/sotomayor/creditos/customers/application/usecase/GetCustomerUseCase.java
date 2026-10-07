package pe.sotomayor.creditos.customers.application.usecase;

import java.util.UUID;

import pe.sotomayor.creditos.customers.application.dto.response.CustomerResponse;

/**
 * Contrato del caso de uso get de Customer.
 *
 * @author DarwinG
 */
public interface GetCustomerUseCase {
    CustomerResponse get(UUID customerCode);
}
