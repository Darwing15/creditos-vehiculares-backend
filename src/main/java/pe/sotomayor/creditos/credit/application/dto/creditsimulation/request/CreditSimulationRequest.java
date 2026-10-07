package pe.sotomayor.creditos.credit.application.dto.creditsimulation.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import java.math.BigDecimal;

/**
 * Entrada no persistente para simular condiciones del financiamiento.
 *
 * @param currency         moneda
 * @param vehiclePrice     precio de venta
 * @param downPayment      cuota inicial
 * @param installmentCount cuotas mensuales
 * @author DarwinG
 */
public record CreditSimulationRequest(
        @NotNull FinancingCurrency currency,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal vehiclePrice,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal downPayment,
        @NotNull @Min(1) @Max(12) Integer installmentCount) {
}
