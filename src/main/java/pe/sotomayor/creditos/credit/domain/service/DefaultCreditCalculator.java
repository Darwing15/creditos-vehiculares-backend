package pe.sotomayor.creditos.credit.domain.service;

import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.math.BigDecimal;

import static pe.sotomayor.creditos.shared.error.ErrorCode.CREDIT_APPLICATION_INVALID_DOWN_PAYMENT;
import static pe.sotomayor.creditos.shared.error.ErrorCode.CREDIT_APPLICATION_INVALID_INSTALLMENT_COUNT;

/**
 * Implementacion por defecto de los calculos financieros
 * del dominio de creditos.
 *
 * <p>
 * Centraliza reglas deterministicas relacionadas con las
 * condiciones financieras utilizadas durante la evaluacion
 * y formalizacion de un financiamiento.
 * </p>
 *
 * <p>
 * Esta implementacion pertenece al dominio y no depende de
 * infraestructura ni de componentes de Spring.
 * </p>
 *
 * @author DarwinG
 */

public class DefaultCreditCalculator implements CreditCalculator {

    private static final int MIN_INSTALLMENT_COUNT = 1;

    private static final int MAX_ZERO_INTEREST_INSTALLMENT_COUNT = 4;

    private static final int MAX_INSTALLMENT_COUNT = 12;

    private static final BigDecimal ZERO_RATE =
            new BigDecimal("0.00");

    private static final BigDecimal FIRST_RANGE_LIMIT =
            new BigDecimal("4500.00");

    private static final BigDecimal SECOND_RANGE_LIMIT =
            new BigDecimal("5500.00");

    private static final BigDecimal THIRD_RANGE_LIMIT =
            new BigDecimal("7000.00");

    private static final BigDecimal FOURTH_RANGE_LIMIT =
            new BigDecimal("9500.00");

    private static final BigDecimal FIFTH_RANGE_LIMIT =
            new BigDecimal("11500.00");

    private static final BigDecimal SIXTH_RANGE_LIMIT =
            new BigDecimal("13500.00");

    private static final BigDecimal FIRST_RANGE_RATE =
            new BigDecimal("2.00");

    private static final BigDecimal SECOND_RANGE_RATE =
            new BigDecimal("1.80");

    private static final BigDecimal THIRD_RANGE_RATE =
            new BigDecimal("1.70");

    private static final BigDecimal FOURTH_RANGE_RATE =
            new BigDecimal("1.60");

    private static final BigDecimal FIFTH_RANGE_RATE =
            new BigDecimal("1.50");

    private static final BigDecimal SIXTH_RANGE_RATE =
            new BigDecimal("1.40");

    private static final BigDecimal SEVENTH_RANGE_RATE =
            new BigDecimal("1.30");

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal calculateFinancedAmount(
            BigDecimal vehiclePrice,
            BigDecimal downPayment) {

        validateDownPayment(
                vehiclePrice,
                downPayment
        );

        return vehiclePrice.subtract(downPayment);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal calculateCompensatoryInterestRate(
            BigDecimal financedAmount,
            Integer installmentCount) {

        validateInstallmentCount(
                installmentCount
        );

        if (installmentCount <= MAX_ZERO_INTEREST_INSTALLMENT_COUNT) {
            return ZERO_RATE;
        }

        return determineCompensatoryInterestRate(
                financedAmount
        );

    }

    /**
     * Valida que la cuota inicial cumpla las reglas necesarias
     * para calcular un monto financiado valido.
     *
     * <p>
     * La cuota inicial debe ser mayor o igual a cero y menor
     * que el precio del vehiculo.
     * </p>
     *
     * @param vehiclePrice precio de venta del vehiculo.
     * @param downPayment  cuota inicial propuesta.
     * @throws ValidationException si la cuota inicial es negativa
     *                             o es mayor o igual al precio
     *                             del vehiculo.
     */
    private void validateDownPayment(
            BigDecimal vehiclePrice,
            BigDecimal downPayment) {

        if (downPayment.compareTo(BigDecimal.ZERO) < 0) {

            throw new ValidationException(
                    CREDIT_APPLICATION_INVALID_DOWN_PAYMENT,
                    "Down payment cannot be negative"
            );
        }

        if (downPayment.compareTo(vehiclePrice) >= 0) {

            throw new ValidationException(
                    CREDIT_APPLICATION_INVALID_DOWN_PAYMENT,
                    "Down payment must be lower than vehicle price"
            );
        }
    }

    /**
     * Valida que el numero de cuotas se encuentre dentro
     * del rango permitido para una operacion de financiamiento.
     *
     * <p>
     * El financiamiento debe pactarse entre 1 y 12 cuotas
     * mensuales, incluyendo ambos limites.
     * </p>
     *
     * @param installmentCount numero de cuotas mensuales pactadas.
     * @throws ValidationException si el numero de cuotas es menor
     *                             que 1 o mayor que 12.
     */
    private void validateInstallmentCount(
            Integer installmentCount) {

        if (installmentCount < MIN_INSTALLMENT_COUNT
                || installmentCount > MAX_INSTALLMENT_COUNT) {

            throw new ValidationException(
                    CREDIT_APPLICATION_INVALID_INSTALLMENT_COUNT,
                    "Installment count must be between 1 and 12"
            );
        }
    }

    /**
     * Determina la tasa compensatoria mensual correspondiente
     * al monto financiado.
     *
     * <p>
     * La tasa se obtiene aplicando los rangos financieros
     * definidos para operaciones pactadas entre 5 y 12 cuotas.
     * </p>
     *
     * @param financedAmount monto financiado de la operacion.
     * @return tasa compensatoria mensual expresada como porcentaje.
     */
    private BigDecimal determineCompensatoryInterestRate(
            BigDecimal financedAmount) {

        if (financedAmount.compareTo(
                FIRST_RANGE_LIMIT) < 0) {
            return FIRST_RANGE_RATE;
        }

        if (financedAmount.compareTo(
                SECOND_RANGE_LIMIT) < 0) {
            return SECOND_RANGE_RATE;
        }

        if (financedAmount.compareTo(
                THIRD_RANGE_LIMIT) < 0) {
            return THIRD_RANGE_RATE;
        }

        if (financedAmount.compareTo(
                FOURTH_RANGE_LIMIT) < 0) {
            return FOURTH_RANGE_RATE;
        }

        if (financedAmount.compareTo(
                FIFTH_RANGE_LIMIT) < 0) {
            return FIFTH_RANGE_RATE;
        }

        if (financedAmount.compareTo(
                SIXTH_RANGE_LIMIT) < 0) {
            return SIXTH_RANGE_RATE;
        }

        return SEVENTH_RANGE_RATE;
    }

}
