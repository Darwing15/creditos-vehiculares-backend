package pe.sotomayor.creditos.credit.application.service.creditsimulation;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import pe.sotomayor.creditos.credit.application.dto.creditsimulation.request.CreditSimulationRequest;

import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import pe.sotomayor.creditos.credit.domain.service.DefaultCreditCalculator;

import pe.sotomayor.creditos.credit.domain.service.installment.DefaultInstallmentCalculator;

import pe.sotomayor.creditos.shared.exception.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class SimulateCreditApplicationServiceTest {

    private final SimulateCreditApplicationService service = new SimulateCreditApplicationService(
            new DefaultCreditCalculator(), new DefaultInstallmentCalculator());

    @Test
    void regularInstallmentUsesDomainRateAndRounding() {

        /* Arrange */

        /* Act */

        var result = service.simulate(
                request(
                        "6500.00",
                        "1500.00",
                        6
                )
        );

        /* Assert */

        assertThat(result.currency())
                .isEqualTo(FinancingCurrency.PEN);

        assertThat(result.financedAmount())
                .isEqualByComparingTo("5000.00");

        assertThat(result.compensatoryInterestRate())
                .isEqualByComparingTo("1.80");

        assertThat(result.monthlyCompensatoryInterestAmount())
                .isEqualByComparingTo("90.00");

        assertThat(result.estimatedMonthlyInstallment())
                .isEqualByComparingTo("924.00");

        assertThat(result.installmentCount())
                .isEqualTo(6);
    }

    @Test
    void fourInstallmentsHaveZeroInterest() {

        /* Arrange */

        /* Act */

        var result = service.simulate(
                request(
                        "6500.00",
                        "1500.00",
                        4
                )
        );

        /* Assert */

        assertThat(result.compensatoryInterestRate())
                .isEqualByComparingTo("0.00");

        assertThat(result.estimatedMonthlyInstallment())
                .isEqualByComparingTo("1250.00");
    }

    @Test
    void excessiveDownPaymentIsRejected() {

        /* Arrange */

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.simulate(
                        request(
                                "6500.00",
                                "6500.00",
                                6)
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void unsupportedTermIsRejected() {

        /* Arrange */

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.simulate(
                        request(
                                "6500.00",
                                "1500.00",
                                24)
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class);
    }

    private CreditSimulationRequest request(String price, String downPayment, int months) {
        return new CreditSimulationRequest(
                FinancingCurrency.PEN,
                new BigDecimal(price),
                new BigDecimal(downPayment),
                months
        );
    }
}

