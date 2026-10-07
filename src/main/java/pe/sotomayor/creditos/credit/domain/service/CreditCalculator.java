package pe.sotomayor.creditos.credit.domain.service;

import java.math.BigDecimal;

/**
 * Define las operaciones de cálculo financiero del dominio de créditos.
 *
 * <p>
 * Centraliza las reglas de negocio relacionadas con los cálculos
 * financieros de una operación de financiamiento, permitiendo
 * reutilizarlas en los distintos casos de uso del módulo.
 * </p>
 *
 * @author DarwinG
 */
public interface CreditCalculator {

    /**
     * Calcula el monto realmente financiado.
     *
     * <p>
     * El monto financiado corresponde al precio del vehículo
     * menos la cuota inicial pagada por el cliente.
     * </p>
     *
     * @param vehiclePrice precio del vehículo.
     * @param downPayment  cuota inicial pagada.
     * @return monto financiado.
     */
    BigDecimal calculateFinancedAmount(
            BigDecimal vehiclePrice,
            BigDecimal downPayment
    );

    /**
     * Calcula la tasa compensatoria mensual aplicable a una
     * operación de financiamiento.
     *
     * <p>
     * La tasa se determina según el número de cuotas pactadas
     * y el monto financiado de la operación.
     * </p>
     *
     * <p>
     * El valor retornado representa un porcentaje. Por ejemplo,
     * {@code 1.50} corresponde a una tasa mensual de 1.50 %.
     * </p>
     *
     * @param financedAmount   monto financiado de la operación.
     * @param installmentCount número de cuotas mensuales pactadas.
     * @return tasa compensatoria mensual expresada como porcentaje.
     */
    BigDecimal calculateCompensatoryInterestRate(
            BigDecimal financedAmount,
            Integer installmentCount
    );

}
