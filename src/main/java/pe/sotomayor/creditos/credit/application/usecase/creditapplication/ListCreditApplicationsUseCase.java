package pe.sotomayor.creditos.credit.application.usecase.creditapplication;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;

import java.util.List;

/**
 * Define el caso de uso para listar solicitudes de crédito
 * según su estado.
 *
 * @author DarwinG
 */
public interface ListCreditApplicationsUseCase {

    /**
     * Obtiene las solicitudes de crédito que se encuentran
     * en el estado indicado.
     *
     * @param status estado de las solicitudes a consultar.
     * @return solicitudes encontradas.
     */
    List<CreditApplicationResponse> listByStatus(
            CreditApplicationStatus status
    );

}