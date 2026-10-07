package pe.sotomayor.creditos.credit.domain.service.installment;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultInstallmentDueDateGeneratorTest {

    private final BusinessDayCalendar businessDayCalendar =
            new DefaultBusinessDayCalendar();

    private final DefaultInstallmentDueDateGenerator generator =
            new DefaultInstallmentDueDateGenerator(
                    businessDayCalendar
            );

    @Test
    void shouldPreserveNominalDayForSubsequentMonthlyDueDates() {

        /* Arrange */

        LocalDate firstDueDate =
                LocalDate.of(
                        2026,
                        10,
                        10
                );

        int installmentCount = 3;

        /* Act */

        List<LocalDate> dueDates =
                generator.generate(
                        firstDueDate,
                        installmentCount,
                        Set.of()
                );

        /* Assert */

        assertThat(dueDates)
                .containsExactly(
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 11, 10),
                        LocalDate.of(2026, 12, 10)
                );
    }

    @Test
    void shouldReturnOnlyFirstDueDateWhenThereIsOneInstallment() {

        /* Arrange */

        LocalDate firstDueDate =
                LocalDate.of(
                        2026,
                        10,
                        10
                );

        int installmentCount = 1;

        /* Act */

        List<LocalDate> dueDates =
                generator.generate(
                        firstDueDate,
                        installmentCount,
                        Set.of()
                );

        /* Assert */

        assertThat(dueDates)
                .containsExactly(
                        firstDueDate
                );
    }

    @Test
    void shouldPreserveOriginalNominalDayAfterShorterMonth() {

        /* Arrange */

        LocalDate firstDueDate =
                LocalDate.of(
                        2026,
                        1,
                        31
                );

        int installmentCount = 3;

        /* Act */

        List<LocalDate> dueDates =
                generator.generate(
                        firstDueDate,
                        installmentCount,
                        Set.of()
                );

        /* Assert */

        assertThat(
                dueDates.get(2)
        )
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                3,
                                31
                        )
                );
    }

    @Test
    void shouldMoveSundayNominalDueDateToNextBusinessDay() {

        /* Arrange */

        LocalDate firstDueDate =
                LocalDate.of(
                        2026,
                        10,
                        15
                );

        int installmentCount = 2;

        Set<LocalDate> applicableHolidays =
                Set.of();

        /* Act */

        List<LocalDate> dueDates =
                generator.generate(
                        firstDueDate,
                        installmentCount,
                        applicableHolidays
                );

        /* Assert */

        assertThat(dueDates)
                .containsExactly(
                        LocalDate.of(
                                2026,
                                10,
                                15
                        ),
                        LocalDate.of(
                                2026,
                                11,
                                16
                        )
                );
    }

    @Test
    void shouldMoveHolidayNominalDueDateToNextBusinessDay() {

        /* Arrange */

        LocalDate firstDueDate =
                LocalDate.of(
                        2026,
                        10,
                        12
                );

        int installmentCount = 1;

        Set<LocalDate> applicableHolidays =
                Set.of(
                        LocalDate.of(
                                2026,
                                10,
                                12
                        )
                );

        /* Act */

        List<LocalDate> dueDates =
                generator.generate(
                        firstDueDate,
                        installmentCount,
                        applicableHolidays
                );

        /* Assert */

        assertThat(dueDates)
                .containsExactly(
                        LocalDate.of(
                                2026,
                                10,
                                13
                        )
                );
    }

    @Test
    void shouldMoveNonexistentNominalDayToNextBusinessDayAndPreserveOriginalNominalDay() {

        /* Arrange */

        LocalDate firstDueDate =
                LocalDate.of(
                        2026,
                        1,
                        31
                );

        int installmentCount = 3;

        Set<LocalDate> applicableHolidays =
                Set.of();

        /* Act */

        List<LocalDate> dueDates =
                generator.generate(
                        firstDueDate,
                        installmentCount,
                        applicableHolidays
                );

        /* Assert */

        assertThat(dueDates)
                .containsExactly(
                        LocalDate.of(
                                2026,
                                1,
                                31
                        ),
                        LocalDate.of(
                                2026,
                                3,
                                2
                        ),
                        LocalDate.of(
                                2026,
                                3,
                                31
                        )
                );
    }

    @Test
    void shouldMoveNonexistentNominalDayTwentyNineToNextMonth() {

        /* Arrange */

        LocalDate firstDueDate =
                LocalDate.of(
                        2027,
                        1,
                        29
                );

        int installmentCount = 2;

        Set<LocalDate> applicableHolidays =
                Set.of();

        /* Act */

        List<LocalDate> dueDates =
                generator.generate(
                        firstDueDate,
                        installmentCount,
                        applicableHolidays
                );

        /* Assert */

        assertThat(dueDates)
                .containsExactly(
                        LocalDate.of(
                                2027,
                                1,
                                29
                        ),
                        LocalDate.of(
                                2027,
                                3,
                                1
                        )
                );
    }
}
