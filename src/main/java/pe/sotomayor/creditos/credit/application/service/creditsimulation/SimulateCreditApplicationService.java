package pe.sotomayor.creditos.credit.application.service.creditsimulation;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.credit.application.dto.creditsimulation.request.CreditSimulationRequest;

import pe.sotomayor.creditos.credit.application.dto.creditsimulation.response.CreditSimulationResponse;

import pe.sotomayor.creditos.credit.application.usecase.creditsimulation.SimulateCreditApplicationUseCase;

import pe.sotomayor.creditos.credit.domain.service.CreditCalculator;

import pe.sotomayor.creditos.credit.domain.service.installment.InstallmentCalculator;

import java.math.BigDecimal;

/**
 * Calcula la estimacion usando las politicas financieras existentes.
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SimulateCreditApplicationService implements SimulateCreditApplicationUseCase {
    private final CreditCalculator creditCalculator;
    private final InstallmentCalculator installmentCalculator;

    /**
     * Calcula la misma cuota regular usada por el cronograma de cuotas.
     *
     * @param request datos de entrada validados
     * @return resultado sin persistencia
     */
    @Override
    public CreditSimulationResponse simulate(CreditSimulationRequest request) {
        BigDecimal financed = creditCalculator.calculateFinancedAmount(
                request.vehiclePrice(), request.downPayment());
        BigDecimal rate = creditCalculator.calculateCompensatoryInterestRate(
                financed, request.installmentCount());
        BigDecimal interest = installmentCalculator.calculateMonthlyCompensatoryInterestAmount(
                financed, rate);
        BigDecimal installment = installmentCalculator.calculateRegularInstallmentAmount(
                financed, request.installmentCount(), interest);
        return new CreditSimulationResponse(request.currency(), financed, rate, interest,
                request.installmentCount(), installment);
    }
}