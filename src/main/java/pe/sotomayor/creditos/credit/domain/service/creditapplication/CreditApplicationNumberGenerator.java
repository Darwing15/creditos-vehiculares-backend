package pe.sotomayor.creditos.credit.domain.service.creditapplication;

/**
 * Define el contrato para generar el número visible
 * de una solicitud de crédito.
 *
 * @author DarwinG
 */
public interface CreditApplicationNumberGenerator {

    /**
     * Genera un nuevo número visible para una solicitud de crédito.
     *
     * @return número visible único.
     */
    String generate();

}
