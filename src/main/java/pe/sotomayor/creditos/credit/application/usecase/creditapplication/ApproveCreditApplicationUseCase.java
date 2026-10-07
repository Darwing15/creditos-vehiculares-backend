package pe.sotomayor.creditos.credit.application.usecase.creditapplication;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import java.util.UUID;

/**
 * Define el caso de uso para aprobar una solicitud de crédito.
 *
 * <p>
 * La aprobación representa la resolución favorable de una
 * solicitud que se encuentra pendiente de evaluación.
 * </p>
 *
 * @author DarwinG
 */
public interface ApproveCreditApplicationUseCase {

    /**
     * Aprueba la solicitud de crédito identificada por su
     * código público.
     *
     * @param applicationCode código público de la solicitud.
     * @return información actualizada de la solicitud aprobada.
     */
    CreditApplicationResponse approve(
            UUID applicationCode
    );

}
