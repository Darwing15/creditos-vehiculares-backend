package pe.sotomayor.creditos.credit.domain.service.creditapplication;

/**
 * Genera números visibles y secuenciales para las solicitudes
 * de crédito.
 *
 * <p>
 * El número se construye a partir de un correlativo proporcionado
 * por {@link CreditApplicationSequenceProvider}, manteniendo al
 * dominio independiente del mecanismo utilizado para obtener
 * dicho valor.
 * </p>
 *
 * @author DarwinG
 */
public class SequentialCreditApplicationNumberGenerator
        implements CreditApplicationNumberGenerator {

    private static final String APPLICATION_NUMBER_FORMAT =
            "CA-%08d";

    private final CreditApplicationSequenceProvider sequenceProvider;

    /**
     * Crea el generador utilizando el proveedor de secuencia indicado.
     *
     * @param sequenceProvider proveedor del siguiente correlativo.
     */
    public SequentialCreditApplicationNumberGenerator(
            CreditApplicationSequenceProvider sequenceProvider) {

        this.sequenceProvider = sequenceProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generate() {

        return APPLICATION_NUMBER_FORMAT.formatted(
                sequenceProvider.nextValue()
        );
    }

}
