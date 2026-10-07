package pe.sotomayor.creditos.credit.application.dto.creditsimulation.response;

import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import java.math.BigDecimal;

/**
 * Resultado estimado, no contractual, de la simulacion.
 *
 * @author DarwinG
 */
public record CreditSimulationResponse(
        FinancingCurrency currency,
        BigDecimal financedAmount,
        BigDecimal compensatoryInterestRate,
        BigDecimal monthlyCompensatoryInterestAmount,
        Integer installmentCount,
        BigDecimal estimatedMonthlyInstallment) {
}
