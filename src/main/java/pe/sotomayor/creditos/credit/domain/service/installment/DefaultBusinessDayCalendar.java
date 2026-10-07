package pe.sotomayor.creditos.credit.domain.service.installment;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Implementación predeterminada de las reglas
 * para determinar días hábiles.
 *
 * @author DarwinG
 */
@Service
public class DefaultBusinessDayCalendar
        implements BusinessDayCalendar {

    @Override
    public boolean isBusinessDay(
            LocalDate date,
            Set<LocalDate> applicableHolidays) {

        return !DayOfWeek.SUNDAY.equals(
                date.getDayOfWeek())
                && !applicableHolidays.contains(
                date);
    }

    @Override
    public LocalDate adjustToBusinessDay(
            LocalDate date,
            Set<LocalDate> applicableHolidays) {

        LocalDate adjustedDate =
                date;

        while (!isBusinessDay(
                adjustedDate,
                applicableHolidays)) {

            adjustedDate =
                    adjustedDate.plusDays(1);
        }

        return adjustedDate;
    }
}
