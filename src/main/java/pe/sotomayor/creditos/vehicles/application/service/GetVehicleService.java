package pe.sotomayor.creditos.vehicles.application.service;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleResponse;
import pe.sotomayor.creditos.vehicles.application.mapper.VehicleMapper;
import pe.sotomayor.creditos.vehicles.application.usecase.GetVehicleUseCase;

import pe.sotomayor.creditos.vehicles.domain.repository.VehicleRepository;

import java.util.UUID;

/**
 * Consulta detalle sin exponer recursos de otras sucursales.
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetVehicleService implements GetVehicleUseCase {

    private final VehicleRepository repository;

    private final VehicleMapper mapper;

    private final CurrentBranchProvider branches;

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasAuthority('vehicles:read')")
    public VehicleResponse get(UUID vehicleCode) {
        return mapper.toResponse(repository
                .findByVehicleCodeAndBranchId(
                        vehicleCode,
                        branches.getCurrentBranchId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND,
                        "Record not found in current branch")
                )
        );
    }
}

