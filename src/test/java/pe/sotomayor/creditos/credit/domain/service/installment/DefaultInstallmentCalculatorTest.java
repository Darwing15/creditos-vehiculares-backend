package pe.sotomayor.creditos.credit.domain.service.installment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultInstallmentCalculatorTest {

    private final DefaultInstallmentCalculator calculator =
            new DefaultInstallmentCalculator();

    @Test
    void shouldRoundMonthlyCompensatoryInterestUpToTwoDecimals() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("3333.33");

        BigDecimal compensatoryInterestRate =
                new BigDecimal("2.00");

        /* Act */

        BigDecimal result =
                calculator.calculateMonthlyCompensatoryInterestAmount(
                        financedAmount,
                        compensatoryInterestRate
                );

        /* Assert */

        assertThat(result)
                .isEqualByComparingTo("66.67");
    }

    @Test
    void shouldKeepMonthlyCompensatoryInterestWhenAlreadyExactToTwoDecimals() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("15000.00");

        BigDecimal compensatoryInterestRate =
                new BigDecimal("1.50");

        /* Act */

        BigDecimal result =
                calculator.calculateMonthlyCompensatoryInterestAmount(
                        financedAmount,
                        compensatoryInterestRate
                );

        /* Assert */

        assertThat(result)
                .isEqualByComparingTo("225.00");
    }

    @Test
    void shouldReturnZeroMonthlyCompensatoryInterestWhenRateIsZero() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("4000.00");

        BigDecimal compensatoryInterestRate =
                new BigDecimal("0.00");

        /* Act */

        BigDecimal result =
                calculator.calculateMonthlyCompensatoryInterestAmount(
                        financedAmount,
                        compensatoryInterestRate
                );

        /* Assert */

        assertThat(result)
                .isEqualByComparingTo("0.00");
    }

    @Test
    void shouldRoundRegularInstallmentAmountUpToNextWholeAmount() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("5000.00");

        int installmentCount = 6;

        BigDecimal monthlyCompensatoryInterestAmount =
                new BigDecimal("90.00");

        /* Act */

        BigDecimal result =
                calculator.calculateRegularInstallmentAmount(
                        financedAmount,
                        installmentCount,
                        monthlyCompensatoryInterestAmount
                );

        /* Assert */

        assertThat(result)
                .isEqualByComparingTo("924.00");
    }

    @Test
    void shouldKeepRegularInstallmentAmountWhenAlreadyWhole() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("6000.00");

        int installmentCount = 6;

        BigDecimal monthlyCompensatoryInterestAmount =
                new BigDecimal("90.00");

        /* Act */

        BigDecimal result =
                calculator.calculateRegularInstallmentAmount(
                        financedAmount,
                        installmentCount,
                        monthlyCompensatoryInterestAmount
                );

        /* Assert */

        assertThat(result)
                .isEqualByComparingTo("1090.00");
    }

    @Test
    void shouldCalculateRegularPrincipalAmount() {

        /* Arrange */

        BigDecimal regularInstallmentAmount =
                new BigDecimal("924.00");

        BigDecimal monthlyCompensatoryInterestAmount =
                new BigDecimal("90.00");

        /* Act */

        BigDecimal result =
                calculator.calculateRegularPrincipalAmount(
                        regularInstallmentAmount,
                        monthlyCompensatoryInterestAmount
                );

        /* Assert */

        assertThat(result)
                .isEqualByComparingTo("834.00");
    }

    @Test
    void shouldCalculateLastPrincipalAmountReconcilingFinancedAmount() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("5000.00");

        int installmentCount = 6;

        BigDecimal regularPrincipalAmount =
                new BigDecimal("834.00");

        /* Act */

        BigDecimal result =
                calculator.calculateLastPrincipalAmount(
                        financedAmount,
                        installmentCount,
                        regularPrincipalAmount
                );

        /* Assert */

        assertThat(result)
                .isEqualByComparingTo("830.00");
    }

    @Test
    void shouldCalculateOutstandingPrincipalAfterInstallment() {

        /* Arrange */

        BigDecimal currentOutstandingPrincipal =
                new BigDecimal("5000.00");

        BigDecimal principalAmount =
                new BigDecimal("834.00");

        /* Act */

        BigDecimal result =
                calculator.calculateOutstandingPrincipal(
                        currentOutstandingPrincipal,
                        principalAmount
                );

        /* Assert */

        assertThat(result)
                .isEqualByComparingTo("4166.00");
    }

    @Test
    void shouldReturnZeroOutstandingPrincipalWhenPrincipalFullyAmortizesBalance() {

        /* Arrange */

        BigDecimal currentOutstandingPrincipal =
                new BigDecimal("830.00");

        BigDecimal principalAmount =
                new BigDecimal("830.00");

        /* Act */

        BigDecimal result =
                calculator.calculateOutstandingPrincipal(
                        currentOutstandingPrincipal,
                        principalAmount
                );

        /* Assert */

        assertThat(result)
                .isEqualByComparingTo("0.00");
    }

    @Test
    void shouldReturnFinancedAmountAsLastPrincipalWhenThereIsOnlyOneInstallment() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("4000.00");

        int installmentCount = 1;

        BigDecimal regularPrincipalAmount =
                new BigDecimal("4000.00");

        /* Act */

        BigDecimal result =
                calculator.calculateLastPrincipalAmount(
                        financedAmount,
                        installmentCount,
                        regularPrincipalAmount
                );

        /* Assert */

        assertThat(result)
                .isEqualByComparingTo("4000.00");
    }

}
