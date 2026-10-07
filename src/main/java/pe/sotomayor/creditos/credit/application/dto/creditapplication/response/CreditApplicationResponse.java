package pe.sotomayor.creditos.credit.application.dto.creditapplication.response;

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;
import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representa la información de una solicitud de crédito.
 *
 * <p>
 * Contiene la identidad pública, las condiciones financieras
 * registradas y el estado actual de la solicitud.
 * </p>
 *
 * @param applicationCode          código público único de la solicitud.
 * @param applicationNumber        número visible de la solicitud.
 * @param customerId               identificador del cliente solicitante.
 * @param vehicleId                identificador del vehículo asociado.
 * @param branchId                 identificador de la sucursal.
 * @param currency                 moneda de la operación de financiamiento.
 * @param vehiclePrice             precio del vehículo registrado.
 * @param downPayment              cuota inicial registrada.
 * @param financedAmount           monto financiado calculado.
 * @param compensatoryInterestRate tasa compensatoria mensual registrada.
 * @param installmentCount         número de cuotas mensuales.
 * @param status                   estado actual de la solicitud.
 * @param resolvedAt               instante en que la solicitud fue resuelta.
 * @param resolvedBy               identificador del usuario que realizó la resolución.
 * @param rejectionReason          motivo de rechazo cuando corresponda.
 * @author DarwinG
 */
public record CreditApplicationResponse(

        UUID applicationCode,

        String applicationNumber,

        Long customerId,

        Long vehicleId,

        Long branchId,

        FinancingCurrency currency,

        BigDecimal vehiclePrice,

        BigDecimal downPayment,

        BigDecimal financedAmount,

        BigDecimal compensatoryInterestRate,

        Integer installmentCount,

        CreditApplicationStatus status,

        OffsetDateTime resolvedAt,

        Long resolvedBy,

        String rejectionReason

) {
}
