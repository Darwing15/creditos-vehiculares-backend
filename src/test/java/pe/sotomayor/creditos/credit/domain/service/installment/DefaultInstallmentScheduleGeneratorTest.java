package pe.sotomayor.creditos.credit.domain.service.installment;

import org.junit.jupiter.api.Test;
import pe.sotomayor.creditos.credit.domain.model.entity.Installment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultInstallmentScheduleGeneratorTest {

    private final InstallmentCalculator calculator =
            new DefaultInstallmentCalculator();

    private final DefaultInstallmentScheduleGenerator generator =
            new DefaultInstallmentScheduleGenerator(
                    calculator,
                    new InstallmentTermsCalculator(calculator)
            );

    @Test
    void shouldGenerateSingleInstallmentSchedule() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("4000.00");

        int installmentCount = 1;

        BigDecimal compensatoryInterestRate =
                new BigDecimal("0.00");

        List<LocalDate> dueDates =
                List.of(
                        LocalDate.of(
                                2026,
                                10,
                                10
                        )
                );

        Long creditId = 1L;

        /* Act */

        List<Installment> installments =
                generator.generate(
                        financedAmount,
                        installmentCount,
                        compensatoryInterestRate,
                        dueDates,
                        creditId
                );

        /* Assert */

        assertThat(installments)
                .hasSize(1);

        Installment installment =
                installments.getFirst();

        assertThat(installment.getInstallmentNumber())
                .isEqualTo(1);

        assertThat(installment.getDueDate())
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                10,
                                10
                        )
                );

        assertThat(installment.getPrincipalAmount())
                .isEqualByComparingTo("4000.00");

        assertThat(installment.getCompensatoryInterestAmount())
                .isEqualByComparingTo("0.00");

        assertThat(installment.getAmount())
                .isEqualByComparingTo("4000.00");

        assertThat(installment.getOutstandingPrincipal())
                .isEqualByComparingTo("0.00");

        assertThat(installment.getBalance())
                .isEqualByComparingTo("4000.00");

        assertThat(installment.getCreditId())
                .isEqualTo(1L);
    }

    @Test
    void shouldGenerateInstallmentsInContractualSequence() {

        /* Arrange */

        /* Act */

        List<Installment> installments =
                generateDefaultSchedule();

        /* Assert */

        assertThat(installments)
                .extracting(
                        Installment::getInstallmentNumber
                )
                .containsExactly(
                        1,
                        2,
                        3,
                        4,
                        5,
                        6
                );
    }

    @Test
    void shouldAssignDueDatesInProvidedOrder() {

        /* Arrange */

        List<LocalDate> dueDates =
                defaultDueDates();

        /* Act */

        List<Installment> installments =
                generateSchedule(dueDates);

        /* Assert */

        assertThat(installments)
                .extracting(
                        Installment::getDueDate
                )
                .containsExactlyElementsOf(
                        dueDates
                );
    }

    @Test
    void shouldUseRegularFinancialValuesForAllInstallmentsExceptLast() {

        /* Arrange */

        int installmentCount = 6;

        /* Act */

        List<Installment> installments =
                generateDefaultSchedule();

        /* Assert */

        assertThat(
                installments.subList(
                        0,
                        installmentCount - 1
                )
        )
                .allSatisfy(
                        installment -> {

                            assertThat(
                                    installment.getPrincipalAmount()
                            )
                                    .isEqualByComparingTo(
                                            "834.00"
                                    );

                            assertThat(
                                    installment.getCompensatoryInterestAmount()
                            )
                                    .isEqualByComparingTo(
                                            "90.00"
                                    );

                            assertThat(
                                    installment.getAmount()
                            )
                                    .isEqualByComparingTo(
                                            "924.00"
                                    );
                        }
                );
    }

    @Test
    void shouldUseReconciledFinancialValuesForLastInstallment() {

        /* Arrange */

        /* Act */

        List<Installment> installments =
                generateDefaultSchedule();

        Installment lastInstallment =
                installments.getLast();

        /* Assert */

        assertThat(
                lastInstallment.getPrincipalAmount()
        )
                .isEqualByComparingTo(
                        "830.00"
                );

        assertThat(
                lastInstallment.getCompensatoryInterestAmount()
        )
                .isEqualByComparingTo(
                        "90.00"
                );

        assertThat(
                lastInstallment.getAmount()
        )
                .isEqualByComparingTo(
                        "920.00"
                );
    }

    @Test
    void shouldReduceOutstandingPrincipalUntilFullyAmortized() {

        /* Arrange */

        /* Act */

        List<Installment> installments =
                generateDefaultSchedule();

        /* Assert */

        assertThat(installments)
                .extracting(
                        Installment::getOutstandingPrincipal
                )
                .usingElementComparator(
                        BigDecimal::compareTo
                )
                .containsExactly(
                        new BigDecimal("4166.00"),
                        new BigDecimal("3332.00"),
                        new BigDecimal("2498.00"),
                        new BigDecimal("1664.00"),
                        new BigDecimal("830.00"),
                        new BigDecimal("0.00")
                );
    }

    @Test
    void shouldReconcileTotalPrincipalWithFinancedAmount() {

        /* Arrange */

        BigDecimal financedAmount =
                new BigDecimal("5000.00");

        /* Act */

        List<Installment> installments =
                generateDefaultSchedule();

        BigDecimal totalPrincipal =
                installments.stream()
                        .map(
                                Installment::getPrincipalAmount
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        /* Assert */

        assertThat(totalPrincipal)
                .isEqualByComparingTo(
                        financedAmount
                );
    }

    @Test
    void shouldRejectGenerationWhenDueDateCountIsLessThanInstallmentCount() {

        /* Arrange */

        List<LocalDate> dueDates =
                List.of(
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 11, 10),
                        LocalDate.of(2026, 12, 10),
                        LocalDate.of(2027, 1, 11),
                        LocalDate.of(2027, 2, 10)
                );

        /* Act */

        /* Assert */

        assertThatThrownBy(
                () -> generateSchedule(
                        dueDates
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Due date count must match installment count"
                );
    }

    @Test
    void shouldRejectGenerationWhenDueDateCountIsGreaterThanInstallmentCount() {

        /* Arrange */

        List<LocalDate> dueDates =
                List.of(
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 11, 10),
                        LocalDate.of(2026, 12, 10),
                        LocalDate.of(2027, 1, 11),
                        LocalDate.of(2027, 2, 10),
                        LocalDate.of(2027, 3, 10),
                        LocalDate.of(2027, 4, 10)
                );

        /* Act */

        /* Assert */

        assertThatThrownBy(
                () -> generateSchedule(
                        dueDates
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Due date count must match installment count"
                );
    }

    private List<Installment> generateDefaultSchedule() {

        return generateSchedule(
                defaultDueDates()
        );
    }

    private List<Installment> generateSchedule(
            List<LocalDate> dueDates) {

        return generator.generate(
                new BigDecimal("5000.00"),
                6,
                new BigDecimal("1.80"),
                dueDates,
                1L
        );
    }

    private List<LocalDate> defaultDueDates() {

        return List.of(
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2027, 1, 11),
                LocalDate.of(2027, 2, 10),
                LocalDate.of(2027, 3, 10)
        );
    }

}
