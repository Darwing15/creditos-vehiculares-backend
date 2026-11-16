package pe.sotomayor.creditos.customers.application.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.customers.application.dto.response.CustomerOptionResponse;
import pe.sotomayor.creditos.customers.application.dto.response.CustomerPageResponse;
import pe.sotomayor.creditos.customers.application.mapper.CustomerMapper;

import pe.sotomayor.creditos.customers.application.usecase.ListCustomersUseCase;

import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import java.util.List;

/**
 * Implementa la lectura de clientes elegibles y limita el resultado a la sucursal autenticada.
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListCustomersService implements ListCustomersUseCase {
    private final CustomerRepository repository;
    private final CustomerMapper mapper;
    private final CurrentBranchProvider branchProvider;

    /**
     * @return clientes activos de la sucursal actual.
     * @throws AccessDeniedException si falta customers:read.
     */
    @Override
    @PreAuthorize("hasAuthority('customers:read')")
    public List<CustomerOptionResponse> listActive() {
        return repository.findAllByBranchIdAndActiveTrue(branchProvider.getCurrentBranchId())
                .stream().map(mapper::toOption).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasAuthority('customers:read')")
    public CustomerPageResponse searchPage(
            String query, int page, int size) {
        String term = query == null ? "" : query.strip();
        if (page < 0 || page > 1_000_000 || size < 1
                || size > 100 || (query != null && query.length() > 150)) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR,
                    "La búsqueda o los parámetros de paginación son inválidos.");
        }
        var result = repository.searchActiveInBranch(
                branchProvider.getCurrentBranchId(), term,
                PageRequest.of(page, size, Sort.by("id")
                        .ascending()));

        return new CustomerPageResponse(
                result.getContent().stream().map(mapper::toOption).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }
}
