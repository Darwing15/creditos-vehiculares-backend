package pe.sotomayor.creditos.credit.application.usecase.creditapplication;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.request
        .CreateCreditApplicationRequest;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response
        .CreditApplicationResponse;

/**
 * Define el caso de uso para registrar nuevas solicitudes
 * de credito.
 *
 * @author DarwinG
 */
public interface CreateCreditApplicationUseCase {

    /**
     * Registra una nueva solicitud de financiamiento.
     *
     * @param request informacion requerida para registrar
     *                la solicitud.
     * @return informacion de la solicitud registrada.
     */
    CreditApplicationResponse create(
            CreateCreditApplicationRequest request
    );

}
