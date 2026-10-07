package pe.sotomayor.creditos.credit.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import pe.sotomayor.creditos.shared.exception.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static pe.sotomayor.creditos.shared.error.ErrorCode.CREDIT_APPLICATION_INVALID_DOWN_PAYMENT;
import static pe.sotomayor.creditos.shared.error.ErrorCode.CREDIT_APPLICATION_INVALID_INSTALLMENT_COUNT;

class DefaultCreditCalculatorTest {

    private final DefaultCreditCalculator calculator =
            new DefaultCreditCalculator();

    @Test
    void shouldCalculateFinancedAmount() {

        /* Arrange */

        BigDecimal vehiclePrice =
                new BigDecimal("12000.00");

        BigDecimal downPayment =
                new BigDecimal("2000.00");

        /* Act */

        BigDecimal financedAmount =
                calculator.calculateFinancedAmount(
                        vehiclePrice,
                        downPayment
                );

        /* Assert */

        assertThat(financedAmount)
                .isEqualByComparingTo("10000.00");
    }

    @Test
    void shouldCalculateFullVehiclePriceWhenDownPaymentIsZero() {

        /* Arrange */

        BigDecimal vehiclePrice =
                new BigDecimal("12000.00");

        BigDecimal downPayment =
                BigDecimal.ZERO;

        /* Act */

        BigDecimal financedAmount =
                calculator.calculateFinancedAmount(
                        vehiclePrice,
                        downPayment
                );

        /* Assert */

        assertThat(financedAmount)
                .isEqualByComparingTo("12000.00");
    }

    @Test
    void shouldRejectNegativeDownPayment() {

        /* Arrange */

        BigDecimal vehiclePrice =
                new BigDecimal("12000.00");

        BigDecimal downPayment =
                new BigDecimal("-1.00");

        /* Act */

        Throwable throwable = catchThrowable(() ->
                calculator.calculateFinancedAmount(
                        vehiclePrice,
                        downPayment
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        CREDIT_APPLICATION_INVALID_DOWN_PAYMENT
                );
    }

    @Test
    void shouldRejectDownPaymentEqualToVehiclePrice() {

        /* Arrange */

        BigDecimal vehiclePrice =
                new BigDecimal("12000.00");

        BigDecimal downPayment =
                new BigDecimal("12000.00");

        /* Act */

        Throwable throwable = catchThrowable(() ->
                calculator.calculateFinancedAmount(
                        vehiclePrice,
                        downPayment
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        CREDIT_APPLICATION_INVALID_DOWN_PAYMENT
                );
    }

    @Test
    void shouldRejectDownPaymentGreaterThanVehiclePrice() {

        /* Arrange */

        BigDecimal vehiclePrice =
                new BigDecimal("12000.00");

        BigDecimal downPayment =
                new BigDecimal("13000.00");

        /* Act */

        Throwable throwable = catchThrowable(() ->
                calculator.calculateFinancedAmount(
                        vehiclePrice,
                        downPayment
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        CREDIT_APPLICATION_INVALID_DOWN_PAYMENT
                );
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void shouldCalculateZeroCompensatoryInterestRateWhenInstallmentCountIsBetweenOneAndFour(
            int installmentCount) {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("10000.00");

        /* Act */

        BigDecimal compensatoryInterestRate =
                calculator.calculateCompensatoryInterestRate(
                        financedAmount,
                        installmentCount
                );

        /* Assert */

        assertThat(compensatoryInterestRate)
                .isEqualByComparingTo("0.00");
    }

    @Test
    void shouldCalculateTwoPercentCompensatoryInterestRateWhenFinancedAmountIsBelowFourThousandFiveHundred() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("4000.00");

        Integer installmentCount = 5;

        /* Act */

        BigDecimal compensatoryInterestRate =
                calculator.calculateCompensatoryInterestRate(
                        financedAmount,
                        installmentCount
                );

        /* Assert */

        assertThat(compensatoryInterestRate)
                .isEqualByComparingTo("2.00");
    }

    @Test
    void shouldCalculateOnePointEightPercentCompensatoryInterestRateWhenFinancedAmountIsFourThousandFiveHundred() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("4500.00");

        Integer installmentCount = 5;

        /* Act */

        BigDecimal compensatoryInterestRate =
                calculator.calculateCompensatoryInterestRate(
                        financedAmount,
                        installmentCount
                );

        /* Assert */

        assertThat(compensatoryInterestRate)
                .isEqualByComparingTo("1.80");
    }

    @Test
    void shouldCalculateOnePointSevenPercentCompensatoryInterestRateWhenFinancedAmountIsFiveThousandFiveHundred() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("5500.00");

        Integer installmentCount = 5;

        /* Act */

        BigDecimal compensatoryInterestRate =
                calculator.calculateCompensatoryInterestRate(
                        financedAmount,
                        installmentCount
                );

        /* Assert */

        assertThat(compensatoryInterestRate)
                .isEqualByComparingTo("1.70");
    }

    @Test
    void shouldCalculateOnePointSixPercentCompensatoryInterestRateWhenFinancedAmountIsSevenThousand() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("7000.00");

        Integer installmentCount = 5;

        /* Act */

        BigDecimal compensatoryInterestRate =
                calculator.calculateCompensatoryInterestRate(
                        financedAmount,
                        installmentCount
                );

        /* Assert */

        assertThat(compensatoryInterestRate)
                .isEqualByComparingTo("1.60");
    }

    @Test
    void shouldCalculateOnePointFivePercentCompensatoryInterestRateWhenFinancedAmountIsNineThousandFiveHundred() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("9500.00");

        Integer installmentCount = 5;

        /* Act */

        BigDecimal compensatoryInterestRate =
                calculator.calculateCompensatoryInterestRate(
                        financedAmount,
                        installmentCount
                );

        /* Assert */

        assertThat(compensatoryInterestRate)
                .isEqualByComparingTo("1.50");
    }

    @Test
    void shouldCalculateOnePointFourPercentCompensatoryInterestRateWhenFinancedAmountIsElevenThousandFiveHundred() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("11500.00");

        Integer installmentCount = 5;

        /* Act */

        BigDecimal compensatoryInterestRate =
                calculator.calculateCompensatoryInterestRate(
                        financedAmount,
                        installmentCount
                );

        /* Assert */

        assertThat(compensatoryInterestRate)
                .isEqualByComparingTo("1.40");
    }

    @Test
    void shouldCalculateOnePointThreePercentCompensatoryInterestRateWhenFinancedAmountIsThirteenThousandFiveHundred() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("13500.00");

        Integer installmentCount = 5;

        /* Act */

        BigDecimal compensatoryInterestRate =
                calculator.calculateCompensatoryInterestRate(
                        financedAmount,
                        installmentCount
                );

        /* Assert */

        assertThat(compensatoryInterestRate)
                .isEqualByComparingTo("1.30");
    }

    @Test
    void shouldCalculateOnePointThreePercentCompensatoryInterestRateWhenFinancedAmountIsFifteenThousandFiveHundred() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("15500.00");

        Integer installmentCount = 5;

        /* Act */

        BigDecimal compensatoryInterestRate =
                calculator.calculateCompensatoryInterestRate(
                        financedAmount,
                        installmentCount
                );

        /* Assert */

        assertThat(compensatoryInterestRate)
                .isEqualByComparingTo("1.30");
    }

    @Test
    void shouldApplySameCompensatoryInterestRateForTwelveInstallments() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("9500.00");

        Integer installmentCount = 12;

        /* Act */

        BigDecimal compensatoryInterestRate =
                calculator.calculateCompensatoryInterestRate(
                        financedAmount,
                        installmentCount
                );

        /* Assert */

        assertThat(compensatoryInterestRate)
                .isEqualByComparingTo("1.50");
    }

    @Test
    void shouldRejectInstallmentCountLowerThanOne() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("9500.00");

        Integer installmentCount = 0;

        /* Act */

        Throwable throwable = catchThrowable(() ->
                calculator.calculateCompensatoryInterestRate(
                        financedAmount,
                        installmentCount
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        CREDIT_APPLICATION_INVALID_INSTALLMENT_COUNT
                );
    }

    @Test
    void shouldRejectInstallmentCountGreaterThanTwelve() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("9500.00");

        Integer installmentCount = 13;

        /* Act */

        Throwable throwable = catchThrowable(() ->
                calculator.calculateCompensatoryInterestRate(
                        financedAmount,
                        installmentCount
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        CREDIT_APPLICATION_INVALID_INSTALLMENT_COUNT
                );
    }

}
