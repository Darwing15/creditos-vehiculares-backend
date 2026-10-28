package pe.sotomayor.creditos.customers.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.customers.application.dto.request.IdentityLookupRequest;
import pe.sotomayor.creditos.customers.application.mapper.IdentityLookupMapper;

import pe.sotomayor.creditos.customers.domain.model.entity.IdentityDocumentCache;
import pe.sotomayor.creditos.customers.domain.model.valueobject.IdentityData;
import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;

import pe.sotomayor.creditos.customers.domain.repository.IdentityDocumentCacheRepository;

import pe.sotomayor.creditos.customers.domain.service.IdentityLookupProvider;
import pe.sotomayor.creditos.customers.domain.service.IdentityLookupBudget;
import pe.sotomayor.creditos.customers.domain.service.IdentityLookupPolicy;
import pe.sotomayor.creditos.customers.domain.service.IdentityLookupCoordinator;

import pe.sotomayor.creditos.customers.domain.exception.IdentityLookupException;

import pe.sotomayor.creditos.shared.security.CurrentUserProvider;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LookupCustomerIdentityServiceTest {

    @Mock
    IdentityDocumentCacheRepository repository;

    @Mock
    IdentityLookupProvider provider;

    @Mock
    IdentityLookupBudget budget;

    @Mock
    IdentityLookupPolicy policy;

    @Mock
    CurrentUserProvider users;

    @Mock
    CurrentBranchProvider branches;

    @Spy
    IdentityLookupMapper mapper = Mappers.getMapper(IdentityLookupMapper.class);

    @Spy
    Clock clock = Clock.fixed(Instant.parse("2026-10-11T15:00:00Z"), ZoneOffset.UTC);

    @Spy
    IdentityLookupCoordinator coordinator = new IdentityLookupCoordinator();

    @InjectMocks
    LookupCustomerIdentityService service;

    @Test
    void shouldReturnCachedNameWhenEntryIsFresh() {

        /* Arrange */

        var request = createRequest();
        var entry = createEntry();
        when(branches.getCurrentBranchId()).thenReturn(3L);
        when(repository.findByDocumentNumber(request.documentNumber()))
                .thenReturn(Optional.of(entry));

        /* Act */

        var response = service.lookup(request);

        /* Assert */

        assertThat(response.fullName())
                .isEqualTo("NOMBRES APELLIDO UNO APELLIDO DOS");
        assertThat(response.fromCache())
                .isTrue();

        verify(branches).getCurrentBranchId();
        verify(repository).findByDocumentNumber(request.documentNumber());
        verifyNoInteractions(provider, budget, policy, users);
        verifyNoMoreInteractions(repository, provider, budget, policy, users, branches);
    }

    @Test
    void shouldPersistExternalResultWhenCacheIsMissing() {

        /* Arrange */

        var request = createRequest();
        var data = createData();
        when(branches.getCurrentBranchId()).thenReturn(3L);
        when(repository.findByDocumentNumber(request.documentNumber()))
                .thenReturn(Optional.empty());
        when(policy.enabled()).thenReturn(true);
        when(policy.cacheTtl()).thenReturn(Duration.ofDays(30));
        when(users.getCurrentUserId()).thenReturn(7L);
        when(provider.lookup(request.documentNumber()))
                .thenReturn(data);

        /* Act */

        var response = service.lookup(request);

        /* Assert */

        assertThat(response.fromCache()).isFalse();
        assertThat(response.documentNumber())
                .isEqualTo(request.documentNumber());
        assertThat(response.expiresAt())
                .isEqualTo(clock.instant().plus(Duration.ofDays(30)));
        verify(branches).getCurrentBranchId();
        verify(repository).findByDocumentNumber(request.documentNumber());
        verify(policy).enabled();
        verify(policy).cacheTtl();
        verify(users).getCurrentUserId();
        verify(budget).reserve(7L, clock.instant());
        verify(provider).lookup(request.documentNumber());
        verify(repository).upsert(any(IdentityDocumentCache.class));
        verifyNoMoreInteractions(repository, provider, budget, policy, users, branches);
    }

    @Test
    void shouldRefreshResultWhenEntryHasExpired() {

        /* Arrange */

        var request = createRequest();
        var entry = IdentityDocumentCache.create(
                createData(),
                clock.instant().minusSeconds(60),
                clock.instant(), 7L
        );
        when(branches.getCurrentBranchId()).thenReturn(3L);
        when(repository.findByDocumentNumber(request.documentNumber()))
                .thenReturn(Optional.of(entry));
        when(policy.enabled()).thenReturn(true);
        when(policy.cacheTtl()).thenReturn(Duration.ofDays(30));
        when(users.getCurrentUserId()).thenReturn(7L);
        when(provider.lookup(request.documentNumber()))
                .thenReturn(createData());

        /* Act */

        var response = service.lookup(request);

        /* Assert */

        assertThat(response.fromCache()).isFalse();
        verify(branches).getCurrentBranchId();
        verify(repository).findByDocumentNumber(request.documentNumber());
        verify(policy).enabled();
        verify(policy).cacheTtl();
        verify(users).getCurrentUserId();
        verify(budget).reserve(7L, clock.instant());
        verify(provider).lookup(request.documentNumber());
        verify(repository).upsert(any(IdentityDocumentCache.class));
        verifyNoMoreInteractions(repository, provider, budget, policy, users, branches);
    }

    @Test
    void shouldRejectDocumentWhenTypeIsCe() {

        /* Arrange */

        var request = new IdentityLookupRequest(CustomerDocumentType.CE, "000000000000001");

        /* Act */

        var action = catchThrowable(() -> service.lookup(request));

        /* Assert */

        assertThat(action).isInstanceOf(ValidationException.class);
        verifyNoInteractions(repository, provider, budget, policy, users, branches);
        verifyNoMoreInteractions(repository, provider, budget, policy, users, branches);
    }

    @Test
    void shouldRejectDocumentWhenNumberHasNonAsciiDigits() {

        /* Arrange */

        var request = new IdentityLookupRequest(CustomerDocumentType.DNI, "１２３４５６７８");

        /* Act */

        var action = catchThrowable(() -> service.lookup(request));

        /* Assert */

        assertThat(action).isInstanceOf(ValidationException.class);
        verifyNoInteractions(repository, provider, budget, policy, users, branches);
        verifyNoMoreInteractions(repository, provider, budget, policy, users, branches);
    }

    @Test
    void shouldAvoidExternalCallWhenIntegrationIsDisabled() {

        /* Arrange */

        var request = createRequest();
        when(branches.getCurrentBranchId()).thenReturn(3L);
        when(repository.findByDocumentNumber(request.documentNumber()))
                .thenReturn(Optional.empty());
        when(policy.enabled()).thenReturn(false);

        /* Act */

        var action = catchThrowable(() -> service.lookup(request));

        /* Assert */

        assertThat(action)
                .isInstanceOf(IdentityLookupException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDENTITY_LOOKUP_DISABLED);
        verify(branches).getCurrentBranchId();
        verify(repository).findByDocumentNumber(request.documentNumber());
        verify(policy).enabled();
        verifyNoInteractions(provider, budget, users);
        verifyNoMoreInteractions(repository, provider, budget, policy, users, branches);
    }

    @Test
    void shouldAvoidExternalCallWhenBudgetIsExhausted() {

        /* Arrange */

        var request = createRequest();
        when(branches.getCurrentBranchId()).thenReturn(3L);
        when(repository.findByDocumentNumber(request.documentNumber()))
                .thenReturn(Optional.empty());
        when(policy.enabled()).thenReturn(true);
        when(users.getCurrentUserId()).thenReturn(7L);
        var now = clock.instant();
        doThrow(IdentityLookupException.quota())
                .when(budget).reserve(7L, now);

        /* Act */

        var action = catchThrowable(() -> service.lookup(request));

        /* Assert */

        assertThat(action).isInstanceOf(IdentityLookupException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDENTITY_QUOTA_EXHAUSTED);
        verify(branches).getCurrentBranchId();
        verify(repository).findByDocumentNumber(request.documentNumber());
        verify(policy).enabled();
        verify(users).getCurrentUserId();
        verify(budget).reserve(7L, clock.instant());
        verifyNoInteractions(provider);
        verifyNoMoreInteractions(repository, provider, budget, policy, users, branches);
    }

    @Test
    void shouldAvoidPersistingResultWhenProviderFails() {

        /* Arrange */

        var request = createRequest();
        when(branches.getCurrentBranchId()).thenReturn(3L);
        when(repository.findByDocumentNumber(request.documentNumber()))
                .thenReturn(Optional.empty());
        when(policy.enabled()).thenReturn(true);
        when(users.getCurrentUserId()).thenReturn(7L);
        when(provider.lookup(request.documentNumber()))
                .thenThrow(IdentityLookupException.unavailable());

        /* Act */

        var action = catchThrowable(() -> service.lookup(request));

        /* Assert */

        assertThat(action).isInstanceOf(IdentityLookupException.class);
        verify(branches).getCurrentBranchId();
        verify(repository).findByDocumentNumber(request.documentNumber());
        verify(policy).enabled();
        verify(users).getCurrentUserId();
        verify(budget).reserve(7L, clock.instant());
        verify(provider).lookup(request.documentNumber());
        verifyNoMoreInteractions(repository, provider, budget, policy, users, branches);
    }

    private IdentityLookupRequest createRequest() {
        return new IdentityLookupRequest(CustomerDocumentType.DNI, "00000001");
    }

    private IdentityData createData() {
        return new IdentityData(
                "00000001",
                "NOMBRES",
                "APELLIDO UNO",
                "APELLIDO DOS",
                "FACTILIZA"
        );
    }

    private IdentityDocumentCache createEntry() {
        return IdentityDocumentCache.create(
                createData(),
                clock.instant().minusSeconds(60),
                clock.instant().plusSeconds(60),
                7L
        );
    }
}

