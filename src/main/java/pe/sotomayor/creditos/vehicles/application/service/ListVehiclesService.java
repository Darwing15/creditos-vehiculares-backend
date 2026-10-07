package pe.sotomayor.creditos.vehicles.application.service;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleOptionResponse;
import pe.sotomayor.creditos.vehicles.application.mapper.VehicleMapper;
import pe.sotomayor.creditos.vehicles.application.usecase.ListVehiclesUseCase;

import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;
import pe.sotomayor.creditos.vehicles.domain.repository.VehicleRepository;

import java.util.List;

/**
 * Consulta inventario por sucursal, opcionalmente filtrado por estado.
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListVehiclesService implements ListVehiclesUseCase {
    private final VehicleRepository repository;
    private final VehicleMapper mapper;
    private final CurrentBranchProvider branchProvider;

    /**
     * @param status estado opcional.
     * @return opciones visibles en la sucursal autenticada.
     */
    @Override
    @PreAuthorize("hasAuthority('vehicles:read')")
    public List<VehicleOptionResponse> list(VehicleStatus status) {
        Long branchId = branchProvider.getCurrentBranchId();
        var vehicles = status == null
                ? repository.findAllByBranchId(branchId)
                : repository.findAllByBranchIdAndStatus(branchId, status);
        return vehicles.stream().map(mapper::toOption).toList();
    }
}
