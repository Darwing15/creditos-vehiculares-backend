package pe.sotomayor.creditos.vehicles.application.service;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import pe.sotomayor.creditos.vehicles.application.dto.request.CreateVehicleRequest;
import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleResponse;
import pe.sotomayor.creditos.vehicles.application.mapper.VehicleMapper;
import pe.sotomayor.creditos.vehicles.application.usecase.CreateVehicleUseCase;

import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;
import pe.sotomayor.creditos.vehicles.domain.repository.VehicleRepository;
import pe.sotomayor.creditos.vehicles.domain.service.VehicleCodeGenerator;

/**
 * Registra en la sucursal autenticada; la BD arbitra duplicados concurrentes.
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CreateVehicleService implements CreateVehicleUseCase {

    private final VehicleRepository repository;

    private final VehicleMapper mapper;

    private final CurrentBranchProvider branches;

    private final VehicleCodeGenerator codeGenerator;

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasAuthority('vehicles:write')")
    public VehicleResponse create(CreateVehicleRequest request) {

        Long branchId = branches.getCurrentBranchId();

        if (repository.existsIdentifiersIncludingDeleted(
                request.chassisNumber().trim(),
                request.engineNumber().trim(),
                0L)) {
            throw new ConflictException(
                    ErrorCode.VEHICLE_ALREADY_EXISTS,
                    "Identifiers already registered");
        }

        Vehicle entity = Vehicle.create(
                codeGenerator.generate(),
                request.vehicleType().trim(),
                request.brand().trim(),
                request.model().trim(),
                request.manufacturingYear(),
                request.color().trim(),
                request.chassisNumber().trim(),
                request.engineNumber().trim(),
                request.duaNumber(),
                request.referencePrice(),
                branchId
        );

        return mapper.toResponse(repository.saveAndFlush(entity));
    }
}

