package pe.sotomayor.creditos.credit.domain.service.installment;

import java.time.LocalDate;
import java.util.Set;

/**
 * Define las reglas para determinar si una fecha
 * corresponde a un día hábil.
 *
 * @author DarwinG
 */
public interface BusinessDayCalendar {

    /**
     * Determina si una fecha corresponde a un día hábil
     * considerando los feriados aplicables.
     *
     * @param date               fecha a evaluar
     * @param applicableHolidays feriados aplicables a la operación
     * @return {@code true} cuando la fecha es hábil;
     * {@code false} en caso contrario
     */
    boolean isBusinessDay(
            LocalDate date,
            Set<LocalDate> applicableHolidays
    );

    /**
     * Ajusta una fecha al primer día hábil disponible.
     *
     * <p>Si la fecha recibida ya corresponde a un día hábil,
     * se conserva sin modificaciones. En caso contrario,
     * se avanza hasta encontrar el siguiente día hábil.</p>
     *
     * @param date               fecha a ajustar
     * @param applicableHolidays feriados aplicables a la operación
     * @return primera fecha hábil desde la fecha recibida
     */
    LocalDate adjustToBusinessDay(
            LocalDate date,
            Set<LocalDate> applicableHolidays
    );
}
