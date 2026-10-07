package pe.sotomayor.creditos.vehicles.application.usecase;

import pe.sotomayor.creditos.vehicles.application.dto.request.CreateVehicleRequest;
import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleResponse;

/**
 * Contrato del caso de uso create de Vehicle.
 *
 * @author DarwinG
 */
public interface CreateVehicleUseCase {
    VehicleResponse create(CreateVehicleRequest request);
}
