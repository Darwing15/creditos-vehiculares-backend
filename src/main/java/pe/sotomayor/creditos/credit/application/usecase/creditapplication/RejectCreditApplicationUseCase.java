package pe.sotomayor.creditos.credit.application.usecase.creditapplication;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.request.RejectCreditApplicationRequest;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import java.util.UUID;

/**
 * Define el caso de uso para rechazar una solicitud de crédito.
 *
 * @author DarwinG
 */
public interface RejectCreditApplicationUseCase {

    /**
     * Rechaza la solicitud identificada por su código público.
     *
     * @param applicationCode código público de la solicitud.
     * @param request información requerida para efectuar el rechazo.
     * @return información actualizada de la solicitud rechazada.
     */
    CreditApplicationResponse reject(
            UUID applicationCode,
            RejectCreditApplicationRequest request
    );

}
