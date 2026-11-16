package pe.sotomayor.creditos.credit.domain.service.installment;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;

/**
 * Calcula y valida la distribucion de capital antes de aceptar una operacion.
 */
@Service
public class InstallmentTermsCalculator {
    private final InstallmentCalculator calculator;

    public InstallmentTermsCalculator(InstallmentCalculator calculator) {
        this.calculator = calculator;
    }

    public Terms calculate(BigDecimal financed, int count, BigDecimal rate) {
        if (count < 1 || count > 12) {
            throw new ValidationException(ErrorCode.CREDIT_APPLICATION_INVALID_INSTALLMENT_COUNT,
                    "Installment count must be between 1 and 12");
        }
        if (financed == null || financed.signum() <= 0) {
            throw new ValidationException(ErrorCode.CREDIT_APPLICATION_INVALID_FINANCED_AMOUNT,
                    "Financed amount must be positive");
        }
        if (rate == null || rate.signum() < 0) {
            throw new ValidationException(ErrorCode.CREDIT_APPLICATION_INVALID_COMPENSATORY_INTEREST_RATE,
                    "Interest rate must not be negative");
        }

        BigDecimal interest = calculator.calculateMonthlyCompensatoryInterestAmount(financed, rate);
        if (count == 1) {
            return new Terms(interest, financed, financed, financed.add(interest));
        }
        BigDecimal installment = calculator.calculateRegularInstallmentAmount(financed, count, interest);
        BigDecimal principal = calculator.calculateRegularPrincipalAmount(installment, interest);
        BigDecimal lastPrincipal = calculator.calculateLastPrincipalAmount(financed, count, principal);
        if (principal.signum() <= 0 || lastPrincipal.signum() <= 0) {
            throw new ValidationException(ErrorCode.CREDIT_APPLICATION_UNFEASIBLE_INSTALLMENTS,
                    "Financed amount and installment count do not yield positive principal in every installment");
        }
        return new Terms(interest, principal, lastPrincipal, installment);
    }

    public record Terms(BigDecimal monthlyInterest, BigDecimal regularPrincipal,
                        BigDecimal lastPrincipal, BigDecimal estimatedInstallment) {
    }
}

