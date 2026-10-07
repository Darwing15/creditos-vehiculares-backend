package pe.sotomayor.creditos.credit.domain.service.installment;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Implementación predeterminada de los cálculos financieros
 * utilizados para construir las cuotas de un crédito.
 *
 * @author DarwinG
 */
@Service
public class DefaultInstallmentCalculator
        implements InstallmentCalculator {

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

    private static final int MONEY_SCALE = 2;

    @Override
    public BigDecimal calculateMonthlyCompensatoryInterestAmount(
            BigDecimal financedAmount,
            BigDecimal compensatoryInterestRate) {

        return financedAmount
                .multiply(
                        compensatoryInterestRate
                )
                .divide(
                        ONE_HUNDRED,
                        MONEY_SCALE,
                        RoundingMode.CEILING
                );
    }

    @Override
    public BigDecimal calculateRegularInstallmentAmount(
            BigDecimal financedAmount,
            int installmentCount,
            BigDecimal monthlyCompensatoryInterestAmount) {

        BigDecimal installmentCountValue =
                BigDecimal.valueOf(
                        installmentCount
                );

        return financedAmount
                .add(
                        monthlyCompensatoryInterestAmount
                                .multiply(
                                        installmentCountValue
                                )
                )
                .divide(
                        installmentCountValue,
                        0,
                        RoundingMode.CEILING
                )
                .setScale(
                        MONEY_SCALE,
                        RoundingMode.UNNECESSARY
                );
    }

    @Override
    public BigDecimal calculateRegularPrincipalAmount(
            BigDecimal regularInstallmentAmount,
            BigDecimal monthlyCompensatoryInterestAmount) {

        return regularInstallmentAmount
                .subtract(
                        monthlyCompensatoryInterestAmount
                );
    }

    @Override
    public BigDecimal calculateLastPrincipalAmount(
            BigDecimal financedAmount,
            int installmentCount,
            BigDecimal regularPrincipalAmount) {

        BigDecimal previousInstallmentCount =
                BigDecimal.valueOf(
                        installmentCount - 1L
                );

        BigDecimal previousPrincipalTotal =
                regularPrincipalAmount
                        .multiply(
                                previousInstallmentCount
                        );

        return financedAmount
                .subtract(
                        previousPrincipalTotal
                );
    }

    @Override
    public BigDecimal calculateOutstandingPrincipal(
            BigDecimal currentOutstandingPrincipal,
            BigDecimal principalAmount) {

        return currentOutstandingPrincipal
                .subtract(
                        principalAmount
                );
    }
}
