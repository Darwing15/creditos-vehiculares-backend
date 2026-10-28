package pe.sotomayor.creditos.customers.application.service;

import java.time.Clock;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import pe.sotomayor.creditos.customers.application.dto.request.IdentityLookupRequest;
import pe.sotomayor.creditos.customers.application.dto.response.IdentityLookupResponse;
import pe.sotomayor.creditos.customers.application.mapper.IdentityLookupMapper;
import pe.sotomayor.creditos.customers.application.usecase.LookupCustomerIdentityUseCase;

import pe.sotomayor.creditos.customers.domain.repository.IdentityDocumentCacheRepository;
import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;
import pe.sotomayor.creditos.customers.domain.service.IdentityLookupProvider;
import pe.sotomayor.creditos.customers.domain.service.IdentityLookupBudget;
import pe.sotomayor.creditos.customers.domain.service.IdentityLookupPolicy;
import pe.sotomayor.creditos.customers.domain.service.IdentityLookupCoordinator;
import pe.sotomayor.creditos.customers.domain.exception.IdentityLookupException;

import pe.sotomayor.creditos.shared.security.CurrentUserProvider;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;

/**
 * Orquesta cache, presupuesto y proveedor sin mantener una transaccion durante la red.
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class LookupCustomerIdentityService implements LookupCustomerIdentityUseCase {

    private final IdentityDocumentCacheRepository repository;
    private final IdentityLookupProvider provider;
    private final IdentityLookupBudget budget;
    private final IdentityLookupPolicy policy;
    private final CurrentUserProvider users;
    private final CurrentBranchProvider branches;
    private final IdentityLookupMapper mapper;
    private final Clock clock;
    private final IdentityLookupCoordinator coordinator;

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasAuthority('customers:write')")
    public IdentityLookupResponse lookup(IdentityLookupRequest request) {
        validateDocument(request);
        branches.getCurrentBranchId();
        return coordinator.execute(request.documentNumber(),
                () -> resolve(request.documentNumber()));
    }

    private IdentityLookupResponse resolve(String number) {
        var now = clock.instant();
        var cached = repository.findByDocumentNumber(number)
                .filter(entry -> entry.isFresh(now));
        if (cached.isPresent())
            return mapper.toResponse(cached.get(), true);
        if (!policy.enabled())
            throw IdentityLookupException.disabled();
        Long actorId = users.getCurrentUserId();
        budget.reserve(actorId, now);
        var data = provider.lookup(number);
        if (!number.equals(data.documentNumber()))
            throw IdentityLookupException.invalidResponse();
        var entry = mapper.toEntry(
                data,
                clock.instant(),
                policy.cacheTtl(),
                actorId
        );
        repository.upsert(entry);

        return mapper.toResponse(entry, false);
    }

    private void validateDocument(IdentityLookupRequest request) {
        if (request == null || request.documentType() != CustomerDocumentType.DNI
                || request.documentNumber() == null || !request.documentNumber().matches("[0-9]{8}"))
            throw new ValidationException(
                    ErrorCode.IDENTITY_DOCUMENT_INVALID,
                    "La consulta requiere un DNI de ocho dígitos.");
    }
}

