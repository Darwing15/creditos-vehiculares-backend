package pe.sotomayor.creditos.credit.domain.service.installment;

import java.math.BigDecimal;

/**
 * Define las operaciones financieras necesarias para calcular
 * los valores contractuales de las cuotas de un crédito.
 *
 * @author DarwinG
 */
public interface InstallmentCalculator {

    /**
     * Calcula el importe mensual del interés compensatorio
     * aplicable al monto financiado.
     *
     * @param financedAmount           monto financiado del crédito
     * @param compensatoryInterestRate tasa compensatoria mensual expresada en porcentaje
     * @return importe mensual del interés compensatorio redondeado a dos decimales hacia arriba
     */
    BigDecimal calculateMonthlyCompensatoryInterestAmount(
            BigDecimal financedAmount,
            BigDecimal compensatoryInterestRate
    );

    /**
     * Calcula el importe contractual de una cuota regular.
     *
     * <p>El resultado se redondea hacia arriba al importe entero
     * inmediatamente superior cuando presenta parte decimal.</p>
     *
     * @param financedAmount                    monto financiado del crédito
     * @param installmentCount                  número total de cuotas
     * @param monthlyCompensatoryInterestAmount importe mensual del interés compensatorio
     * @return importe contractual de una cuota regular
     */
    BigDecimal calculateRegularInstallmentAmount(
            BigDecimal financedAmount,
            int installmentCount,
            BigDecimal monthlyCompensatoryInterestAmount
    );

    /**
     * Calcula el importe de capital correspondiente
     * a una cuota regular.
     *
     * @param regularInstallmentAmount          importe contractual de la cuota regular
     * @param monthlyCompensatoryInterestAmount importe mensual del interés compensatorio
     * @return importe de capital de la cuota regular
     */
    BigDecimal calculateRegularPrincipalAmount(
            BigDecimal regularInstallmentAmount,
            BigDecimal monthlyCompensatoryInterestAmount
    );

    /**
     * Calcula el importe de capital correspondiente
     * a la última cuota.
     *
     * <p>El último capital reconcilia las diferencias producidas
     * por el redondeo de las cuotas regulares, de modo que la suma
     * total del capital sea igual al monto financiado.</p>
     *
     * @param financedAmount         monto financiado del crédito
     * @param installmentCount       número total de cuotas
     * @param regularPrincipalAmount importe de capital de una cuota regular
     * @return importe de capital correspondiente a la última cuota
     */
    BigDecimal calculateLastPrincipalAmount(
            BigDecimal financedAmount,
            int installmentCount,
            BigDecimal regularPrincipalAmount
    );

    /**
     * Calcula el saldo de capital pendiente del crédito
     * después de la amortización correspondiente a una cuota.
     *
     * @param currentOutstandingPrincipal saldo de capital antes de la amortización
     * @param principalAmount             capital amortizado por la cuota
     * @return saldo de capital pendiente después de la amortización
     */
    BigDecimal calculateOutstandingPrincipal(
            BigDecimal currentOutstandingPrincipal,
            BigDecimal principalAmount
    );
}
