package pe.sotomayor.creditos.credit.domain.service.installment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class InstallmentTermsCalculatorTest {
    private final InstallmentCalculator calculator = new DefaultInstallmentCalculator();
    private final InstallmentTermsCalculator terms = new InstallmentTermsCalculator(calculator);
    private final DefaultInstallmentScheduleGenerator generator =
            new DefaultInstallmentScheduleGenerator(calculator, terms);

    @ParameterizedTest
    @CsvSource({"0.01,2,0", "1.00,2,0", "3.00,5,2", "1.00,12,2"})
    void rejectsWhenFinalPrincipalIsNotPositive(String amount, int count, String rate) {

        /* Arrange */

        var principal = new BigDecimal(amount);
        var interest = new BigDecimal(rate);

        /* Act */

        var failure = catchThrowable(() -> terms.calculate(principal, count, interest));

        /* Assert */

        assertThat(failure).isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREDIT_APPLICATION_UNFEASIBLE_INSTALLMENTS);
    }

    @Test
    void acceptsOneCentOfRemainingPrincipalAndReconcilesSchedule() {

        /* Arrange */

        var dates = List.of(LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 12, 10));

        /* Act */

        var result = generator.generate(new BigDecimal("1.01"),
                2, BigDecimal.ZERO, dates, 1L);

        /* Assert */

        assertThat(result.get(0).getPrincipalAmount())
                .isEqualByComparingTo("1.00");
        assertThat(result.get(1).getPrincipalAmount())
                .isEqualByComparingTo("0.01");
        assertThat(result.get(1).getOutstandingPrincipal())
                .isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsUnfeasibleScheduleBeforeBuildingInstallments() {

        /* Arrange */

        var dates = List.of(LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 12, 10));

        /* Act */

        var failure = catchThrowable(() -> generator.generate(new BigDecimal("1.00"),
                2, BigDecimal.ZERO, dates, 1L));

        /* Assert */

        assertThat(failure).isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREDIT_APPLICATION_UNFEASIBLE_INSTALLMENTS);
    }

    @Test
    void singleInstallmentPreservesCents() {

        /* Arrange */

        var amount = new BigDecimal("100.01");

        /* Act */

        var result = terms.calculate(amount, 1, BigDecimal.ZERO);

        /* Assert */

        assertThat(result.lastPrincipal())
                .isEqualByComparingTo("100.01");
        assertThat(result.estimatedInstallment())
                .isEqualByComparingTo("100.01");
    }
}
