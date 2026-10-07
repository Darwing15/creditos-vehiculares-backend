package pe.sotomayor.creditos.customers.application.service;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.customers.application.dto.response.CustomerResponse;
import pe.sotomayor.creditos.customers.application.mapper.CustomerMapper;
import pe.sotomayor.creditos.customers.application.usecase.GetCustomerUseCase;
import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import java.util.UUID;

/**
 * Consulta detalle sin exponer recursos de otras sucursales.
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCustomerService implements GetCustomerUseCase {

    private final CustomerRepository repository;

    private final CustomerMapper mapper;

    private final CurrentBranchProvider branches;

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasAuthority('customers:read')")
    public CustomerResponse get(UUID customerCode) {
        return mapper.toResponse(repository.findByCustomerCodeAndBranchId(
                        customerCode,
                        branches.getCurrentBranchId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND,
                        "Record not found in current branch")));
    }
}

