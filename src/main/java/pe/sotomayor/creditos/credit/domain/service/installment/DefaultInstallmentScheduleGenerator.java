package pe.sotomayor.creditos.credit.domain.service.installment;

import org.springframework.stereotype.Service;
import pe.sotomayor.creditos.credit.domain.model.entity.Installment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación predeterminada para la generación
 * del cronograma contractual de cuotas.
 *
 * @author DarwinG
 */
@Service
public class DefaultInstallmentScheduleGenerator
        implements InstallmentScheduleGenerator {

    private final InstallmentCalculator calculator;

    public DefaultInstallmentScheduleGenerator(
            InstallmentCalculator calculator) {

        this.calculator = calculator;
    }

    @Override
    public List<Installment> generate(
            BigDecimal financedAmount,
            int installmentCount,
            BigDecimal compensatoryInterestRate,
            List<LocalDate> dueDates,
            Long creditId) {

        if (dueDates.size() != installmentCount) {
            throw new IllegalArgumentException(
                    "Due date count must match installment count"
            );
        }

        BigDecimal monthlyCompensatoryInterestAmount =
                calculator.calculateMonthlyCompensatoryInterestAmount(
                        financedAmount,
                        compensatoryInterestRate
                );

        BigDecimal regularInstallmentAmount =
                calculator.calculateRegularInstallmentAmount(
                        financedAmount,
                        installmentCount,
                        monthlyCompensatoryInterestAmount
                );

        BigDecimal regularPrincipalAmount =
                calculator.calculateRegularPrincipalAmount(
                        regularInstallmentAmount,
                        monthlyCompensatoryInterestAmount
                );

        BigDecimal lastPrincipalAmount =
                calculator.calculateLastPrincipalAmount(
                        financedAmount,
                        installmentCount,
                        regularPrincipalAmount
                );

        List<Installment> installments =
                new ArrayList<>();

        BigDecimal currentOutstandingPrincipal =
                financedAmount;

        for (int index = 0;
             index < installmentCount;
             index++) {

            boolean lastInstallment =
                    index == installmentCount - 1;

            BigDecimal principalAmount =
                    lastInstallment
                            ? lastPrincipalAmount
                            : regularPrincipalAmount;

            BigDecimal outstandingPrincipal =
                    calculator.calculateOutstandingPrincipal(
                            currentOutstandingPrincipal,
                            principalAmount
                    );

            Installment installment =
                    Installment.create(
                            index + 1,
                            dueDates.get(index),
                            principalAmount,
                            monthlyCompensatoryInterestAmount,
                            outstandingPrincipal,
                            creditId
                    );

            installments.add(
                    installment
            );

            currentOutstandingPrincipal =
                    outstandingPrincipal;
        }

        return installments;
    }
}
