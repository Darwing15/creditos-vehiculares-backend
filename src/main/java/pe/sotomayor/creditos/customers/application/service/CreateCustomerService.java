package pe.sotomayor.creditos.customers.application.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;

import pe.sotomayor.creditos.customers.application.dto.response.CustomerResponse;
import pe.sotomayor.creditos.customers.application.mapper.CustomerMapper;
import pe.sotomayor.creditos.customers.domain.model.entity.Customer;
import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;


import pe.sotomayor.creditos.customers.application.dto.request.CreateCustomerRequest;
import pe.sotomayor.creditos.customers.application.usecase.CreateCustomerUseCase;
import pe.sotomayor.creditos.customers.domain.service.CustomerCodeGenerator;

/**
 * Registra en la sucursal autenticada; la BD arbitra duplicados concurrentes.
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CreateCustomerService implements CreateCustomerUseCase {

    private final CustomerRepository repository;

    private final CustomerMapper mapper;

    private final CurrentBranchProvider branches;

    private final CustomerCodeGenerator codeGenerator;

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasAuthority('customers:write')")
    public CustomerResponse create(CreateCustomerRequest request) {
        Long branchId = branches.getCurrentBranchId();

        if (repository.existsDocumentIncludingDeleted(
                request.documentType().name(),
                request.documentNumber().trim())) {
            throw new ConflictException(
                    ErrorCode.CUSTOMER_ALREADY_EXISTS,
                    "Identifiers already registered");
        }

        Customer entity = Customer.create(
                codeGenerator.generate(),
                request.fullName().trim(),
                request.documentType(),
                request.documentNumber().trim(),
                request.phone(),
                request.email(),
                request.occupation(),
                request.monthlyIncome(),
                request.address(),
                request.mapReference(),
                branchId
        );

        return mapper.toResponse(repository.saveAndFlush(entity));
    }
}

