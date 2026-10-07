package pe.sotomayor.creditos.credit.domain.service.installment;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementación predeterminada para la generación
 * de fechas contractuales de vencimiento.
 *
 * @author DarwinG
 */
@Service
public class DefaultInstallmentDueDateGenerator
        implements InstallmentDueDateGenerator {

    private final BusinessDayCalendar businessDayCalendar;

    public DefaultInstallmentDueDateGenerator(
            BusinessDayCalendar businessDayCalendar) {

        this.businessDayCalendar =
                businessDayCalendar;
    }

    @Override
    public List<LocalDate> generate(
            LocalDate firstDueDate,
            int installmentCount,
            Set<LocalDate> applicableHolidays) {

        List<LocalDate> dueDates =
                new ArrayList<>();

        int nominalDay =
                firstDueDate.getDayOfMonth();

        YearMonth firstDueMonth =
                YearMonth.from(
                        firstDueDate
                );

        for (int index = 0;
             index < installmentCount;
             index++) {

            YearMonth dueMonth =
                    firstDueMonth.plusMonths(
                            index
                    );

            LocalDate nominalDueDate =
                    resolveNominalDueDate(
                            dueMonth,
                            nominalDay
                    );

            LocalDate dueDate =
                    businessDayCalendar.adjustToBusinessDay(
                            nominalDueDate,
                            applicableHolidays
                    );

            dueDates.add(
                    dueDate
            );
        }

        return dueDates;
    }

    /**
     * Resuelve la fecha nominal de vencimiento para un mes determinado.
     *
     * <p>Cuando el día nominal existe en el mes, se conserva.
     * Si el día nominal no existe, se utiliza el primer día calendario
     * posterior al cierre de dicho mes para que posteriormente sea
     * ajustado al siguiente día hábil aplicable.</p>
     *
     * @param dueMonth   mes contractual de vencimiento
     * @param nominalDay día nominal de vencimiento
     * @return fecha nominal resuelta antes del ajuste a día hábil
     */
    private LocalDate resolveNominalDueDate(
            YearMonth dueMonth,
            int nominalDay) {

        if (nominalDay <= dueMonth.lengthOfMonth()) {

            return dueMonth.atDay(
                    nominalDay
            );
        }

        return dueMonth
                .plusMonths(1)
                .atDay(1);
    }
}
