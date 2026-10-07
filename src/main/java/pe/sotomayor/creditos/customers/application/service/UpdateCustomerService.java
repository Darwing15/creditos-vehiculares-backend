package pe.sotomayor.creditos.customers.application.service;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.customers.application.dto.request.UpdateCustomerRequest;
import pe.sotomayor.creditos.customers.application.dto.response.CustomerResponse;
import pe.sotomayor.creditos.customers.application.mapper.CustomerMapper;
import pe.sotomayor.creditos.customers.application.usecase.UpdateCustomerUseCase;

import pe.sotomayor.creditos.customers.domain.model.entity.Customer;
import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;

import pe.sotomayor.creditos.shared.concurrency.ResourceRevision;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import java.util.UUID;

/**
 * Bloquea la fila, verifica revision y actualiza datos permitidos.
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateCustomerService implements UpdateCustomerUseCase {

    private final CustomerRepository repository;

    private final CustomerMapper mapper;

    private final CurrentBranchProvider branches;

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasAuthority('customers:write')")
    public CustomerResponse update(
            UUID customerCode,
            UpdateCustomerRequest request) {

        Customer entity = repository.findByCustomerCodeAndBranchIdForUpdate(
                        customerCode,
                        branches.getCurrentBranchId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND,
                        "Record not found in current branch"));

        ResourceRevision.requireMatch(
                request.expectedRevision(),
                mapper.revision(entity));

        entity.updateProfile(
                request.fullName().trim(),
                request.phone(),
                request.email(),
                request.occupation(),
                request.monthlyIncome(),
                request.address(),
                request.mapReference()
        );

        return mapper.toResponse(repository.saveAndFlush(entity));
    }
}

