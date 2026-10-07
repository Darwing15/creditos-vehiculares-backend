package pe.sotomayor.creditos.credit.application.usecase.creditapplication;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import java.util.UUID;

/**
 * Define el caso de uso para obtener una solicitud de crédito
 * mediante su identificador público.
 *
 * @author DarwinG
 */
public interface GetCreditApplicationUseCase {

    /**
     * Obtiene la solicitud identificada por su código público.
     *
     * @param applicationCode código público de la solicitud.
     * @return información de la solicitud encontrada.
     */
    CreditApplicationResponse getByApplicationCode(
            UUID applicationCode
    );

}
