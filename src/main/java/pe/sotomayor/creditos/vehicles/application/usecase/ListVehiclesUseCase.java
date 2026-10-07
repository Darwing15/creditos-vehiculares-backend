package pe.sotomayor.creditos.vehicles.application.usecase;

import java.util.List;

import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleOptionResponse;
import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;

/**
 * Caso de uso de consulta de inventario visible para la sucursal actual.
 *
 * @author DarwinG
 */
public interface ListVehiclesUseCase {
    /**
     * @param status filtro opcional; null incluye todos los estados.
     * @return unidades no eliminadas de la sucursal actual.
     */
    List<VehicleOptionResponse> list(VehicleStatus status);
}
