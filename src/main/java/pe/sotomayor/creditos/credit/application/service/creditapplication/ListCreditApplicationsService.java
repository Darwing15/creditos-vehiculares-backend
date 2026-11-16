package pe.sotomayor.creditos.credit.application.service.creditapplication;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationPageItem;
import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationPageResponse;
import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.application.mapper.creditapplication.CreditApplicationMapper;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.ListCreditApplicationsUseCase;

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;
import pe.sotomayor.creditos.credit.domain.repository.CreditApplicationRepository;

import pe.sotomayor.creditos.customers.domain.model.entity.Customer;
import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;
import pe.sotomayor.creditos.vehicles.domain.repository.VehicleRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementa el caso de uso para listar solicitudes de credito
 * segun su estado.
 *
 * <p>
 * El servicio consulta las solicitudes correspondientes y transforma
 * cada agregado encontrado en su representacion de respuesta.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListCreditApplicationsService
        implements ListCreditApplicationsUseCase {

    private final CurrentBranchProvider
            currentBranchProvider;

    private final CreditApplicationRepository
            creditApplicationRepository;

    private final CreditApplicationMapper
            creditApplicationMapper;

    private final CustomerRepository
            customerRepository;

    private final VehicleRepository
            vehicleRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CreditApplicationResponse> listByStatus(
            CreditApplicationStatus status) {

        return creditApplicationRepository
                .findAllByStatusAndBranchId(
                        status,
                        currentBranchProvider.getCurrentBranchId()
                )
                .stream()
                .map(creditApplicationMapper::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasAuthority('credit-applications:read')")
    public CreditApplicationPageResponse searchPage(
            CreditApplicationStatus status, String query, int page, int size) {

        if (status == null || page < 0 || page > 1_000_000 || size < 1 || size > 100
                || (query != null && query.length() > 150)) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR,
                    "La búsqueda o los parámetros de paginación son inválidos.");
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();

        boolean customersAllowed = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("customers:read"));

        boolean vehiclesAllowed = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("vehicles:read"));

        Long branchId = currentBranchProvider.getCurrentBranchId();

        var result = creditApplicationRepository
                .searchPage(branchId, status.name(), query == null ? "" : query.strip(),
                        customersAllowed, vehiclesAllowed, PageRequest.of(page, size));

        var customerIds = result.getContent().stream()
                .map(a -> a.getCustomerId()).distinct().toList();

        var vehicleIds = result.getContent().stream()
                .map(a -> a.getVehicleId()).distinct().toList();

        Map<Long, Customer> customers = customersAllowed && !customerIds.isEmpty()
                ? customerRepository.findAllByBranchIdAndIdInAndActiveTrue(branchId, customerIds)
                .stream().collect(Collectors.toMap(Customer::getId, Function.identity())) : Map.of();

        Map<Long, Vehicle> vehicles = vehiclesAllowed && !vehicleIds.isEmpty()
                ? vehicleRepository.findAllByBranchIdAndIdIn(branchId, vehicleIds).stream()
                .collect(Collectors.toMap(Vehicle::getId, Function.identity())) : Map.of();

        var items = result.getContent().stream().map(a -> {
            var customer = customers.get(a.getCustomerId());
            var vehicle = vehicles.get(a.getVehicleId());
            String vehicleName = vehicle == null ? null :
                    vehicle.getVehicleType() + " " +
                            vehicle.getBrand() + " " +
                            vehicle.getModel();

            return new CreditApplicationPageItem(creditApplicationMapper.toResponse(a),
                    customer == null ? null : customer.getFullName(), vehicleName);
        }).toList();

        return new CreditApplicationPageResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

}
