package pe.sotomayor.creditos.credit.domain.model.enumeration;

/**
 * Representa los estados posibles dentro del ciclo de vida
 * de una solicitud de crédito.
 *
 * <p>
 * Una solicitud comienza en estado {@link #PENDING} y puede
 * avanzar hacia uno de los estados terminales definidos por
 * el proceso de resolución.
 * </p>
 *
 * <p>
 * Los estados {@link #APPROVED}, {@link #REJECTED},
 * {@link #CANCELLED} y {@link #EXPIRED} son terminales y
 * no permiten nuevas transiciones.
 * </p>
 *
 * @author DarwinG
 */
public enum CreditApplicationStatus {

    /**
     * Solicitud registrada y pendiente de resolución definitiva.
     */
    PENDING,

    /**
     * Solicitud que recibió una resolución favorable y queda
     * habilitada para continuar el proceso de formalización.
     */
    APPROVED,

    /**
     * Solicitud evaluada y rechazada.
     */
    REJECTED,

    /**
     * Solicitud cuyo proceso de financiamiento fue cancelado
     * antes de convertirse en una obligación financiera.
     */
    CANCELLED,

    /**
     * Solicitud que perdió su vigencia conforme a las condiciones
     * de expiración definidas por el negocio.
     */
    EXPIRED;

    /**
     * Determina si es válida la transición desde este estado
     * hacia el estado indicado.
     *
     * <p>
     * El único estado desde el cual pueden producirse transiciones
     * es {@link #PENDING}. Una vez resuelta la solicitud, su estado
     * se considera terminal.
     * </p>
     *
     * @param target estado destino
     * @return {@code true} si la transición está permitida;
     * {@code false} en caso contrario
     */
    public boolean canTransitionTo(
            CreditApplicationStatus target) {

        if (target == null) {
            return false;
        }

        return switch (this) {

            case PENDING ->
                    target == APPROVED
                            || target == REJECTED
                            || target == CANCELLED
                            || target == EXPIRED;

            case APPROVED,
                 REJECTED,
                 CANCELLED,
                 EXPIRED ->
                    false;
        };
    }
}
