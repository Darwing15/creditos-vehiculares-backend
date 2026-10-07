package pe.sotomayor.creditos.vehicles.application.usecase;

import java.util.UUID;

import pe.sotomayor.creditos.vehicles.application.dto.request.UpdateVehicleRequest;
import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleResponse;

/**
 * Contrato del caso de uso update de Vehicle.
 *
 * @author DarwinG
 */
public interface UpdateVehicleUseCase {
    VehicleResponse update(
            UUID vehicleCode,
            UpdateVehicleRequest request
    );
}
