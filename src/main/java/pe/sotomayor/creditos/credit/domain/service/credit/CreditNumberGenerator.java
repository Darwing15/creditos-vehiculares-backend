package pe.sotomayor.creditos.credit.domain.service.credit;

/**
 * Define el contrato para la generación del número visible
 * de un crédito.
 *
 * <p>
 * La estrategia de generación puede evolucionar sin afectar
 * los casos de uso del dominio.
 * </p>
 *
 * <p>
 * Ejemplos:
 * </p>
 *
 * <ul>
 *     <li>CRE-000001</li>
 *     <li>CRE-2026-000001</li>
 *     <li>LIM-CRE-000001</li>
 * </ul>
 *
 * @author DarwinG
 */
public interface CreditNumberGenerator {

    /**
     * Genera un nuevo número de crédito.
     *
     * @return número único del crédito.
     */
    String generate();

}
