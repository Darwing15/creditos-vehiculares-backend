package pe.sotomayor.creditos.credit.domain.service.credit;

import java.util.UUID;

/**
 * Implementación por defecto del generador de códigos públicos
 * para créditos.
 *
 * <p>
 * Genera un identificador único universal (UUID) que permite
 * identificar públicamente un crédito sin exponer el identificador
 * interno de la base de datos.
 * </p>
 *
 * <p>
 * La estrategia de generación podrá evolucionar en el futuro sin
 * afectar a los casos de uso que dependen del contrato
 * {@link CreditCodeGenerator}.
 * </p>
 *
 * @author DarwinG
 */

public class UuidCreditCodeGenerator implements CreditCodeGenerator {

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID generate() {

        return UUID.randomUUID();

    }

}
