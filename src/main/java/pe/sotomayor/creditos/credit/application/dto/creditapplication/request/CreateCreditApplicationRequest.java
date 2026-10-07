package pe.sotomayor.creditos.credit.application.dto.creditapplication.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import java.math.BigDecimal;

/**
 * Solicitud para registrar una nueva solicitud de crédito vehicular.
 *
 * <p>
 * Contiene únicamente la información proporcionada durante
 * el registro de la solicitud. Los identificadores públicos,
 * el monto financiado, la tasa compensatoria y el estado
 * inicial son determinados por el sistema.
 * </p>
 *
 * @param customerId       identificador del cliente solicitante.
 * @param vehicleId        identificador del vehículo a financiar.
 * @param currency         moneda de la operación de financiamiento.
 * @param vehiclePrice     precio de venta del vehículo.
 * @param downPayment      cuota inicial propuesta.
 * @param installmentCount número de cuotas mensuales solicitadas.
 * @author DarwinG
 */
public record CreateCreditApplicationRequest(

        @NotNull
        Long customerId,

        @NotNull
        Long vehicleId,

        @NotNull
        FinancingCurrency currency,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal vehiclePrice,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal downPayment,

        @NotNull
        Integer installmentCount

) {
}
