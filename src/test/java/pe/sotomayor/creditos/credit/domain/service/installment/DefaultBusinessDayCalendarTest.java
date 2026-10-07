package pe.sotomayor.creditos.credit.domain.service.installment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultBusinessDayCalendarTest {

    private final DefaultBusinessDayCalendar calendar =
            new DefaultBusinessDayCalendar();

    @Test
    void shouldTreatApplicableHolidayAsNonBusinessDay() {

        /* Arrange */

        LocalDate date =
                LocalDate.of(
                        2026,
                        10,
                        12
                );

        Set<LocalDate> applicableHolidays =
                Set.of(
                        date
                );

        /* Act */

        boolean businessDay =
                calendar.isBusinessDay(
                        date,
                        applicableHolidays
                );

        /* Assert */

        assertThat(businessDay)
                .isFalse();
    }

    @Test
    void shouldTreatSundayAsNonBusinessDay() {

        /* Arrange */

        LocalDate date =
                LocalDate.of(
                        2026,
                        10,
                        11
                );

        Set<LocalDate> applicableHolidays =
                Set.of();

        /* Act */

        boolean businessDay =
                calendar.isBusinessDay(
                        date,
                        applicableHolidays
                );

        /* Assert */

        assertThat(businessDay)
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2026-10-12",
            "2026-10-13",
            "2026-10-14",
            "2026-10-15",
            "2026-10-16",
            "2026-10-17"
    })
    void shouldTreatMondayThroughSaturdayAsBusinessDaysWhenNotHoliday(
            String dateValue) {

        /* Arrange */

        LocalDate date =
                LocalDate.parse(
                        dateValue
                );

        Set<LocalDate> applicableHolidays =
                Set.of();

        /* Act */

        boolean businessDay =
                calendar.isBusinessDay(
                        date,
                        applicableHolidays
                );

        /* Assert */

        assertThat(businessDay)
                .isTrue();
    }

    @Test
    void shouldMoveSundayToNextBusinessDay() {

        /* Arrange */

        LocalDate date =
                LocalDate.of(
                        2026,
                        10,
                        11
                );

        Set<LocalDate> applicableHolidays =
                Set.of();

        /* Act */

        LocalDate adjustedDate =
                calendar.adjustToBusinessDay(
                        date,
                        applicableHolidays
                );

        /* Assert */

        assertThat(adjustedDate)
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                10,
                                12
                        )
                );
    }

    @Test
    void shouldKeepDateWhenAlreadyBusinessDay() {

        /* Arrange */

        LocalDate date =
                LocalDate.of(
                        2026,
                        10,
                        12
                );

        Set<LocalDate> applicableHolidays =
                Set.of();

        /* Act */

        LocalDate adjustedDate =
                calendar.adjustToBusinessDay(
                        date,
                        applicableHolidays
                );

        /* Assert */

        assertThat(adjustedDate)
                .isEqualTo(
                        date
                );
    }

    @Test
    void shouldMoveThroughConsecutiveNonBusinessDaysUntilNextBusinessDay() {

        /* Arrange */

        LocalDate date =
                LocalDate.of(
                        2026,
                        10,
                        17
                );

        Set<LocalDate> applicableHolidays =
                Set.of(
                        LocalDate.of(
                                2026,
                                10,
                                17
                        ),
                        LocalDate.of(
                                2026,
                                10,
                                19
                        )
                );

        /* Act */

        LocalDate adjustedDate =
                calendar.adjustToBusinessDay(
                        date,
                        applicableHolidays
                );

        /* Assert */

        assertThat(adjustedDate)
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                10,
                                20
                        )
                );
    }
}