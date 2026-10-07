package pe.sotomayor.creditos.vehicles.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;

/**
 * Detalle de la ficha y revision para su actualizacion.
 *
 * @author DarwinG
 */
public record VehicleResponse(

        Long id,

        UUID vehicleCode,

        String vehicleType,

        String brand,

        String model,

        Integer manufacturingYear,

        String color,

        String chassisNumber,

        String engineNumber,

        String duaNumber,

        BigDecimal referencePrice,

        Long branchId,

        VehicleStatus status,

        String revision
) {
}
