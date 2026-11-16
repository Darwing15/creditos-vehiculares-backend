package pe.sotomayor.creditos.credit.application.service.creditsimulation;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import pe.sotomayor.creditos.credit.application.dto.creditsimulation.request.CreditSimulationRequest;

import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import pe.sotomayor.creditos.credit.domain.service.DefaultCreditCalculator;

import pe.sotomayor.creditos.credit.domain.service.installment.DefaultInstallmentCalculator;
import pe.sotomayor.creditos.credit.domain.service.installment.InstallmentTermsCalculator;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class SimulateCreditApplicationServiceTest {

    private final SimulateCreditApplicationService service = new SimulateCreditApplicationService(
            new DefaultCreditCalculator(), new InstallmentTermsCalculator(new DefaultInstallmentCalculator()));

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

    @Test
    void oneInstallmentUsesReconciledPrincipal() {

        /* Arrange */

        var input = request("3500.00", "3399.99", 1);

        /* Act */

        var result = service.simulate(input);

        /* Assert */

        assertThat(result.estimatedMonthlyInstallment())
                .isEqualByComparingTo("100.01");
    }

    @Test
    void rejectsUnfeasibleInstallments() {

        /* Arrange */

        var input = request("3500.00", "3499.00", 2);

        /* Act */

        Throwable thrown = catchThrowable(() -> service.simulate(input));

        /* Assert */

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREDIT_APPLICATION_UNFEASIBLE_INSTALLMENTS);
    }

    @Test
    void rateUsesFinancedAmountForBothCurrencies() {

        /* Arrange */

        var requests = Arrays.stream(FinancingCurrency.values())
                .map(currency -> new CreditSimulationRequest(currency,
                new BigDecimal("16000.00"), new BigDecimal("6000.00"), 5)).toList();

        /* Act */

        var results = requests.stream().map(service::simulate).toList();

        /* Assert */

        assertThat(results).allSatisfy(result -> assertThat(result.compensatoryInterestRate())
                .isEqualByComparingTo("1.50"));
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

