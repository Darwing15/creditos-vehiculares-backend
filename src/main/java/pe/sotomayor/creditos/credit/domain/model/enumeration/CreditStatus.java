package pe.sotomayor.creditos.credit.domain.model.enumeration;

/**
 * Representa los estados posibles de un crédito
 * durante el ciclo de vida de la obligación financiera.
 *
 * <p>
 * Un crédito existe únicamente después de la formalización
 * contractual de una operación de financiamiento.
 * Los estados de evaluación y aprobación pertenecen a
 * {@code CreditApplication} y no forman parte de este ciclo.
 * </p>
 *
 * @author DarwinG
 */
public enum CreditStatus {

    /**
     * Obligación financiera vigente y al día.
     */
    ACTIVE,

    /**
     * Obligación financiera vigente con obligaciones vencidas.
     */
    IN_ARREARS,

    /**
     * Obligación financiera completamente satisfecha.
     */
    PAID_OFF

}
