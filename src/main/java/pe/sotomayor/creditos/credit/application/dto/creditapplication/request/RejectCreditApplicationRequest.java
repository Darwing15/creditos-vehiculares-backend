package pe.sotomayor.creditos.credit.application.dto.creditapplication.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud para rechazar una solicitud de crédito.
 *
 * <p>
 * Contiene el motivo proporcionado por el usuario responsable
 * de resolver desfavorablemente la solicitud.
 * </p>
 *
 * @param rejectionReason motivo del rechazo.
 *
 * @author DarwinG
 */
public record RejectCreditApplicationRequest(

        @NotBlank
        String rejectionReason

) {
}
