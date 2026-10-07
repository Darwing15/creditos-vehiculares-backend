package pe.sotomayor.creditos.credit.domain.service.creditapplication;

import java.util.UUID;

/**
 * Genera códigos públicos UUID para las solicitudes de crédito.
 *
 * <p>
 * La implementación utiliza identificadores UUID aleatorios
 * para desacoplar la identidad pública de la solicitud de su
 * identificador interno de persistencia.
 * </p>
 *
 * @author DarwinG
 */
public class UuidCreditApplicationCodeGenerator
        implements CreditApplicationCodeGenerator {

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID generate() {

        return UUID.randomUUID();
    }

}
