package pe.sotomayor.creditos.customers.application.usecase;

import java.util.UUID;

import pe.sotomayor.creditos.customers.application.dto.request.UpdateCustomerRequest;
import pe.sotomayor.creditos.customers.application.dto.response.CustomerResponse;

/**
 * Contrato del caso de uso update de Customer.
 *
 * @author DarwinG
 */
public interface UpdateCustomerUseCase {
    CustomerResponse update(
            UUID customerCode,
            UpdateCustomerRequest request
    );
}
