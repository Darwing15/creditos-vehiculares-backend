package pe.sotomayor.creditos.credit.domain.service.installment;

import pe.sotomayor.creditos.credit.domain.model.entity.Installment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Define la generación del cronograma contractual
 * de cuotas de un crédito.
 *
 * @author DarwinG
 */
public interface InstallmentScheduleGenerator {

    /**
     * Genera las cuotas contractuales correspondientes a un crédito.
     *
     * @param financedAmount           monto financiado
     * @param installmentCount         número total de cuotas
     * @param compensatoryInterestRate tasa compensatoria mensual expresada en porcentaje
     * @param dueDates                 fechas de vencimiento previamente resueltas
     * @param creditId                 identificador interno del crédito
     * @return cronograma contractual generado
     */
    List<Installment> generate(
            BigDecimal financedAmount,
            int installmentCount,
            BigDecimal compensatoryInterestRate,
            List<LocalDate> dueDates,
            Long creditId
    );
}
