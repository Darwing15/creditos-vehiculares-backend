package pe.sotomayor.creditos.vehicles.application.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleOptionResponse;
import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleOverviewResponse;
import pe.sotomayor.creditos.vehicles.application.dto.response.VehiclePageResponse;

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

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasAuthority('vehicles:read')")
    public VehiclePageResponse searchPage(
            String query, VehicleStatus status,
            String brand, int page, int size) {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 100
                || (query != null && query.length() > 150)
                || (brand != null && brand.length() > 100)) {
            throw new ValidationException(
                    ErrorCode.VALIDATION_ERROR,
                    "La búsqueda o los parámetros de paginación son inválidos.");
        }
        String term = query == null ? "" : query.strip();
        var result = repository.searchPage(
                branchProvider.getCurrentBranchId(), term,
                status == null ? "" : status.name(),
                brand == null ? "" : brand,
                PageRequest.of(page, size));

        return new VehiclePageResponse(
                result.getContent().stream()
                        .map(mapper::toOption).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasAuthority('vehicles:read')")
    public VehicleOverviewResponse overview() {
        Long branchId = branchProvider.getCurrentBranchId();
        long total = 0, available = 0, reserved = 0;
        for (Object[] row : repository.countByStatus(branchId)) {
            var status = (VehicleStatus) row[0];
            long count = (Long) row[1];
            total += count;
            if (status == VehicleStatus.AVAILABLE) available = count;
            if (status == VehicleStatus.RESERVED) reserved = count;
        }
        return new VehicleOverviewResponse(
                total,
                available,
                reserved,
                repository.findBrands(branchId)
        );
    }
}
