package pe.sotomayor.creditos.credit.domain.service.credit;

import lombok.RequiredArgsConstructor;

/**
 * Implementación por defecto del generador del número
 * visible de un crédito.
 *
 * <p>
 * El número se construye a partir de un consecutivo
 * obtenido desde un proveedor de secuencias y un
 * prefijo identificador del documento.
 * </p>
 *
 * @author DarwinG
 */

@RequiredArgsConstructor
public class SequentialCreditNumberGenerator
        implements CreditNumberGenerator {

    private static final String PREFIX = "CR-";

    private static final int NUMBER_LENGTH = 8;

    private final CreditSequenceProvider sequenceProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public String generate() {

        long sequence = sequenceProvider.nextValue();

        return PREFIX + String.format(
                "%0" + NUMBER_LENGTH + "d",
                sequence
        );

    }

}
