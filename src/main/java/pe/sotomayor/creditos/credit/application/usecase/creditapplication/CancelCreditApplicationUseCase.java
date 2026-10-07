package pe.sotomayor.creditos.credit.application.usecase.creditapplication;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import java.util.UUID;

/**
 * Define el caso de uso para cancelar una solicitud de crédito.
 *
 * <p>
 * La cancelación representa el desistimiento del proceso
 * de financiamiento antes de la existencia de una obligación
 * financiera.
 * </p>
 *
 * @author DarwinG
 */
public interface CancelCreditApplicationUseCase {

    /**
     * Cancela la solicitud identificada por su código público.
     *
     * @param applicationCode código público de la solicitud.
     * @return información actualizada de la solicitud cancelada.
     */
    CreditApplicationResponse cancel(
            UUID applicationCode
    );

}
