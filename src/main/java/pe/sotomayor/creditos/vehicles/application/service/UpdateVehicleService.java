package pe.sotomayor.creditos.vehicles.application.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;

import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleResponse;
import pe.sotomayor.creditos.vehicles.application.mapper.VehicleMapper;

import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;
import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;
import pe.sotomayor.creditos.vehicles.domain.repository.VehicleRepository;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.concurrency.ResourceRevision;

import java.util.UUID;

import pe.sotomayor.creditos.vehicles.application.dto.request.UpdateVehicleRequest;
import pe.sotomayor.creditos.vehicles.application.usecase.UpdateVehicleUseCase;

/**
 * Bloquea la fila, verifica revision y actualiza datos permitidos.
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateVehicleService implements UpdateVehicleUseCase {

    private final VehicleRepository repository;

    private final VehicleMapper mapper;

    private final CurrentBranchProvider branches;

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasAuthority('vehicles:write')")
    public VehicleResponse update(UUID vehicleCode,
                                  UpdateVehicleRequest request) {

        Vehicle entity = repository
                .findByVehicleCodeAndBranchIdForUpdate(
                        vehicleCode,
                        branches.getCurrentBranchId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND,
                        "Record not found in current branch"));

        ResourceRevision.requireMatch(
                request.expectedRevision(),
                mapper.revision(entity));

        if (entity.getStatus() != VehicleStatus.AVAILABLE) {
            throw new ConflictException(
                    ErrorCode.VEHICLE_NOT_EDITABLE,
                    "Only AVAILABLE vehicles can be updated");
        }
        if (repository.existsIdentifiersIncludingDeleted(
                request.chassisNumber().trim(),
                request.engineNumber().trim(),
                entity.getId())) {
            throw new ConflictException(
                    ErrorCode.VEHICLE_ALREADY_EXISTS,
                    "Identifiers already registered");
        }

        entity.updateDetails(
                request.vehicleType().trim(),
                request.brand().trim(),
                request.model().trim(),
                request.manufacturingYear(),
                request.color().trim(),
                request.chassisNumber().trim(),
                request.engineNumber().trim(),
                request.duaNumber(),
                request.referencePrice()
        );

        return mapper.toResponse(repository.saveAndFlush(entity));
    }
}

