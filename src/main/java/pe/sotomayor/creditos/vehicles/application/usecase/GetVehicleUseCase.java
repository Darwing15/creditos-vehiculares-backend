package pe.sotomayor.creditos.vehicles.application.usecase;

import java.util.UUID;

import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleResponse;

/**
 * Contrato del caso de uso get de Vehicle.
 *
 * @author DarwinG
 */
public interface GetVehicleUseCase {
    VehicleResponse get(UUID vehicleCode);
}
