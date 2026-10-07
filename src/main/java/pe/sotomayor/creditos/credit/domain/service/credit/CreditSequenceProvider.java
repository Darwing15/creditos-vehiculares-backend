package pe.sotomayor.creditos.credit.domain.service.credit;

/**
 * Define el contrato para obtener el siguiente valor
 * secuencial utilizado en la generación del número
 * visible de un crédito.
 *
 * <p>
 * La estrategia de obtención del consecutivo es independiente
 * de la tecnología utilizada para persistir la información.
 * </p>
 *
 * <p>
 * La implementación podrá utilizar secuencias de base de datos,
 * tablas de numeración u otros mecanismos sin afectar a los
 * consumidores de este contrato.
 * </p>
 *
 * @author DarwinG
 */
public interface CreditSequenceProvider {

    /**
     * Obtiene el siguiente valor secuencial disponible.
     *
     * @return siguiente consecutivo.
     */
    long nextValue();

}
