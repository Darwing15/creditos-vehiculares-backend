package pe.sotomayor.creditos.credit.domain.service.creditapplication;

import java.util.UUID;

/**
 * Define el contrato para generar el código público único
 * de una solicitud de crédito.
 *
 * <p>
 * El código permite identificar públicamente una
 * CreditApplication sin exponer su identificador interno
 * de persistencia.
 * </p>
 *
 * @author DarwinG
 */
public interface CreditApplicationCodeGenerator {

    /**
     * Genera un nuevo código público para una solicitud de crédito.
     *
     * @return código público único.
     */
    UUID generate();

}
