package pe.sotomayor.creditos.credit.application.service.creditapplication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.request.RejectCreditApplicationRequest;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.application.mapper.creditapplication.CreditApplicationMapper;

import pe.sotomayor.creditos.credit.domain.model.entity.CreditApplication;

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;

import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import pe.sotomayor.creditos.credit.domain.repository.CreditApplicationRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.shared.security.CurrentUserProvider;

import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;
import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;
import pe.sotomayor.creditos.vehicles.domain.repository.VehicleRepository;

import java.math.BigDecimal;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RejectCreditApplicationServiceTest {

    private static final UUID APPLICATION_CODE =
            UUID.fromString(
                    "550e8400-e29b-41d4-a716-446655440001"
            );

    private static final Long CURRENT_USER_ID = 7L;

    private static final String REJECTION_REASON =
            "Insufficient payment capacity";

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-08-26T15:00:00Z");

    @Mock
    private CurrentBranchProvider
            currentBranchProvider;

    @Mock
    private CreditApplicationRepository
            creditApplicationRepository;

    @Mock
    private CreditApplicationMapper
            creditApplicationMapper;

    @Mock
    private CurrentUserProvider
            currentUserProvider;

    private Clock clock;

    @Mock
    private VehicleRepository
            vehicleRepository;

    private RejectCreditApplicationService service;

    @BeforeEach
    void setUp() {

        clock = Clock.fixed(
                FIXED_INSTANT,
                ZoneOffset.UTC
        );

        service =
                new RejectCreditApplicationService(
                        currentBranchProvider,
                        creditApplicationRepository,
                        creditApplicationMapper,
                        currentUserProvider,
                        clock,
                        vehicleRepository
                );
    }

    @Test
    void shouldRejectPendingCreditApplication() {

        /* Arrange */

        when(currentBranchProvider.getCurrentBranchId())
                .thenReturn(3L);

        RejectCreditApplicationRequest request =
                createRequest();

        CreditApplication application =
                createPendingApplication();

        OffsetDateTime expectedResolvedAt =
                OffsetDateTime.ofInstant(
                        FIXED_INSTANT,
                        ZoneOffset.UTC
                );

        CreditApplicationResponse response =
                createRejectedResponse(
                        expectedResolvedAt
                );

        when(creditApplicationRepository
                .findByApplicationCodeAndBranchIdForUpdate(
                        APPLICATION_CODE,
                        3L))
                .thenReturn(Optional.of(application));

        when(currentUserProvider.getCurrentUserId())
                .thenReturn(CURRENT_USER_ID);

        when(creditApplicationRepository.save(application))
                .thenReturn(application);

        when(creditApplicationMapper.toResponse(application))
                .thenReturn(response);

        Vehicle vehicle = BeanUtils.instantiateClass(Vehicle.class);
        ReflectionTestUtils.setField(vehicle, "status", VehicleStatus.RESERVED);

        when(vehicleRepository.findByIdAndBranchIdForUpdate(2L, 3L))
                .thenReturn(Optional.of(vehicle));

        /* Act */

        CreditApplicationResponse result =
                service.reject(
                        APPLICATION_CODE,
                        request
                );

        /* Assert */

        assertThat(vehicle.getStatus())
                .isEqualTo(VehicleStatus.AVAILABLE);

        verify(vehicleRepository)
                .save(vehicle);

        assertThat(result)
                .isSameAs(response);

        assertThat(application.getStatus())
                .isEqualTo(
                        CreditApplicationStatus.REJECTED
                );

        assertThat(application.getResolvedAt())
                .isEqualTo(expectedResolvedAt);

        assertThat(application.getResolvedBy())
                .isEqualTo(CURRENT_USER_ID);

        assertThat(application.getRejectionReason())
                .isEqualTo(REJECTION_REASON);

        verify(creditApplicationRepository)
                .save(application);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCreditApplicationDoesNotExist() {

        /* Arrange */

        when(currentBranchProvider.getCurrentBranchId())
                .thenReturn(3L);

        RejectCreditApplicationRequest request =
                createRequest();

        when(creditApplicationRepository
                .findByApplicationCodeAndBranchIdForUpdate(
                        APPLICATION_CODE,
                        3L))
                .thenReturn(Optional.empty());

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.reject(
                        APPLICATION_CODE,
                        request
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_NOT_FOUND
                );

        verifyNoInteractions(
                currentUserProvider,
                creditApplicationMapper
        );

        verify(creditApplicationRepository, never())
                .save(any());
    }

    @Test
    void shouldThrowConflictExceptionWhenCreditApplicationIsAlreadyRejected() {

        /* Arrange */

        when(currentBranchProvider.getCurrentBranchId())
                .thenReturn(3L);

        RejectCreditApplicationRequest request =
                createRequest();

        CreditApplication application =
                createRejectedApplication();

        when(creditApplicationRepository
                .findByApplicationCodeAndBranchIdForUpdate(
                        APPLICATION_CODE,
                        3L))
                .thenReturn(Optional.of(application));

        when(currentUserProvider.getCurrentUserId())
                .thenReturn(CURRENT_USER_ID);

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.reject(
                        APPLICATION_CODE,
                        request
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_INVALID_STATUS_TRANSITION
                );

        verify(creditApplicationRepository, never())
                .save(any());

        verifyNoInteractions(
                creditApplicationMapper
        );
    }

    private RejectCreditApplicationRequest createRequest() {

        return new RejectCreditApplicationRequest(
                REJECTION_REASON
        );
    }

    private CreditApplication createPendingApplication() {

        return CreditApplication.create(
                APPLICATION_CODE,
                "CA-00000001",
                FinancingCurrency.PEN,
                new BigDecimal("12000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("1.50"),
                12,
                1L,
                2L,
                3L
        );
    }

    private CreditApplicationResponse createRejectedResponse(
            OffsetDateTime resolvedAt) {

        return new CreditApplicationResponse(
                APPLICATION_CODE,
                "CA-00000001",
                1L,
                2L,
                3L,
                FinancingCurrency.PEN,
                new BigDecimal("12000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("1.50"),
                12,
                CreditApplicationStatus.REJECTED,
                resolvedAt,
                CURRENT_USER_ID,
                REJECTION_REASON
        );
    }

    private CreditApplication createRejectedApplication() {

        CreditApplication application =
                createPendingApplication();

        application.reject(
                OffsetDateTime.ofInstant(
                        FIXED_INSTANT.minusSeconds(3600),
                        ZoneOffset.UTC
                ),
                CURRENT_USER_ID,
                REJECTION_REASON
        );

        return application;
    }

}
