package pe.sotomayor.creditos.credit.domain.service.credit;

import java.util.UUID;

/**
 * Define el contrato para la generación del código público
 * único de un crédito.
 *
 * <p>
 * El código identifica de forma única un crédito dentro del
 * sistema y puede ser utilizado en integraciones externas,
 * APIs y consultas públicas sin exponer el identificador
 * interno de la base de datos.
 * </p>
 *
 * <p>
 * La estrategia de generación puede evolucionar sin afectar
 * los casos de uso del dominio.
 * </p>
 *
 * @author DarwinG
 */
public interface CreditCodeGenerator {

    /**
     * Genera un nuevo código público para un crédito.
     *
     * @return código único del crédito.
     */
    UUID generate();

}
