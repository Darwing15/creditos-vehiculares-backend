package pe.sotomayor.creditos.credit.domain.model.enumeration;

/**
 * Representa el estado de pago derivado de una cuota.
 *
 * <p>El estado no se persiste como un atributo independiente,
 * sino que se determina a partir del importe contractual y del
 * saldo pendiente de la cuota.</p>
 *
 * @author DarwinG
 */
public enum InstallmentPaymentStatus {

    PENDING,
    PARTIALLY_PAID,
    PAID

}
