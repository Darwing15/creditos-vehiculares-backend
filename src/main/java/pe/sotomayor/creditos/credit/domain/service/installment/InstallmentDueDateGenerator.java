package pe.sotomayor.creditos.credit.domain.service.installment;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Define la generación de las fechas contractuales
 * de vencimiento de las cuotas de un crédito.
 *
 * @author DarwinG
 */
public interface InstallmentDueDateGenerator {

    /**
     * Genera las fechas mensuales de vencimiento
     * a partir de la primera fecha contractual.
     *
     * @param firstDueDate     primera fecha contractual de vencimiento
     * @param installmentCount número total de cuotas
     * @return fechas contractuales de vencimiento
     */
    List<LocalDate> generate(
            LocalDate firstDueDate,
            int installmentCount,
            Set<LocalDate> applicableHolidays
    );
}
