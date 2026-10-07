package pe.sotomayor.creditos.credit.application.usecase.creditsimulation;

import pe.sotomayor.creditos.credit.application.dto.creditsimulation.request.CreditSimulationRequest;

import pe.sotomayor.creditos.credit.application.dto.creditsimulation.response.CreditSimulationResponse;

/**
 * Caso de uso sin efectos persistentes para simular una solicitud.
 *
 * @author DarwinG
 */
public interface SimulateCreditApplicationUseCase {
    /**
     * @param request condiciones tentativas @return importe, tasa y cuota estimados
     */
    CreditSimulationResponse simulate(CreditSimulationRequest request);
}
