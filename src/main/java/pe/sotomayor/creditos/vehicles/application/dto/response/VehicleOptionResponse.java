package pe.sotomayor.creditos.vehicles.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;

/**
 * Datos de inventario necesarios para seleccionar o rotular una unidad.
 *
 * @author DarwinG
 */
public record VehicleOptionResponse(

        Long id,

        UUID vehicleCode,

        String vehicleType,

        String brand,

        String model,

        Integer manufacturingYear,

        String color,

        String chassisNumber,

        String engineNumber,

        BigDecimal referencePrice,

        Long branchId,

        VehicleStatus status
) {
}
