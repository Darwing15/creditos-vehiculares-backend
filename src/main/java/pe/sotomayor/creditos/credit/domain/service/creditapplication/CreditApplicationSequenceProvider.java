package pe.sotomayor.creditos.credit.domain.service.creditapplication;

/**
 * Define el contrato para obtener el siguiente valor secuencial
 * utilizado en la generación del número visible de una
 * solicitud de crédito.
 *
 * <p>
 * El contrato mantiene al dominio independiente del mecanismo
 * utilizado para obtener el consecutivo.
 * </p>
 *
 * @author DarwinG
 */
public interface CreditApplicationSequenceProvider {

    /**
     * Obtiene el siguiente valor secuencial disponible.
     *
     * @return siguiente consecutivo.
     */
    long nextValue();

}