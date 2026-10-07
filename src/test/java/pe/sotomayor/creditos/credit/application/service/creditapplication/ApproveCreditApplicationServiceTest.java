package pe.sotomayor.creditos.credit.application.service.creditapplication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.application.mapper.creditapplication.CreditApplicationMapper;

import pe.sotomayor.creditos.credit.domain.model.entity.CreditApplication;

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;
import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import pe.sotomayor.creditos.credit.domain.repository.CreditApplicationRepository;

import pe.sotomayor.creditos.customers.application.dto.request.CreateCustomerRequest;

import pe.sotomayor.creditos.customers.domain.model.entity.Customer;
import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;
import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.shared.security.CurrentUserProvider;

import pe.sotomayor.creditos.vehicles.application.dto.request.CreateVehicleRequest;
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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproveCreditApplicationServiceTest {

    @Mock
    private CurrentBranchProvider branches;

    @Mock
    private CurrentUserProvider users;

    @Mock
    private CreditApplicationRepository applications;

    @Mock
    private CustomerRepository customers;

    @Mock
    private VehicleRepository vehicles;


    private final Clock clock =
            Clock.fixed(Instant.parse("2026-10-01T15:00:00Z"), ZoneOffset.UTC);

    private final CreditApplicationMapper mapper =
            Mappers.getMapper(CreditApplicationMapper.class);

    private ApproveCreditApplicationService
            service;

    @BeforeEach
    void setUp() {

        service =
                new ApproveCreditApplicationService(
                        branches,
                        applications,
                        mapper,
                        users,
                        clock,
                        customers,
                        vehicles
                );
    }

    @Test
    void shouldApproveAndPreserveFinancialConditionsAndReservation() {

        /* Arrange */

        CreditApplication application = createApplication();
        Vehicle vehicle = createVehicle();
        stubEligible(application, vehicle);
        when(users.getCurrentUserId()).thenReturn(7L);
        when(applications.save(application)).thenReturn(application);

        /* Act */

        CreditApplicationResponse result =
                service.approve(application.getApplicationCode());

        /* Assert */

        assertThat(result.status())
                .isEqualTo(CreditApplicationStatus.APPROVED);
        assertThat(result.resolvedBy())
                .isEqualTo(7L);
        assertThat(result.resolvedAt())
                .isEqualTo(OffsetDateTime.now(clock));
        assertThat(result.rejectionReason())
                .isNull();
        assertThat(result.vehiclePrice())
                .isEqualByComparingTo("12000.00");
        assertThat(result.financedAmount())
                .isEqualByComparingTo("10000.00");
        assertThat(result.compensatoryInterestRate())
                .isEqualByComparingTo("1.50");
        assertThat(result.installmentCount())
                .isEqualTo(12);
        assertThat(vehicle.getStatus())
                .isEqualTo(VehicleStatus.RESERVED);

        verifyEligibleLookups(application);
        verify(users)
                .getCurrentUserId();
        verify(applications)
                .save(application);
        verifyNoMoreInteractions(
                branches,
                applications,
                customers,
                vehicles,
                users
        );
    }

    @Test
    void shouldHideMissingOrOtherBranchApplication() {
        /* Arrange */
        UUID code = createApplicationCode();
        when(branches.getCurrentBranchId())
                .thenReturn(3L);
        when(applications.findByApplicationCodeAndBranchIdForUpdate(
                code,
                3L))
                .thenReturn(Optional.empty());

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.approve(code)
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREDIT_APPLICATION_NOT_FOUND);

        verify(branches)
                .getCurrentBranchId();
        verify(applications)
                .findByApplicationCodeAndBranchIdForUpdate(
                        code,
                        3L);
        verifyNoInteractions(
                customers,
                vehicles,
                users
        );
        verifyNoMoreInteractions(
                branches,
                applications
        );
    }

    @ParameterizedTest
    @EnumSource(value = CreditApplicationStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED", "EXPIRED"})
    void shouldRejectEveryTerminalStateBeforeCheckingEligibility(CreditApplicationStatus status) {
        /* Arrange */
        CreditApplication application = createApplication();
        ReflectionTestUtils.setField(application, "status", status);
        stubApplication(application);

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.approve(application.getApplicationCode())
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREDIT_APPLICATION_INVALID_STATUS_TRANSITION);
        assertThat(application.getStatus())
                .isEqualTo(status);

        verifyApplicationLookup(application);
        verifyNoInteractions(
                customers,
                vehicles,
                users
        );
        verifyNoMoreInteractions(
                branches,
                applications
        );
    }

    @Test
    void shouldRejectInactiveDeletedMissingOrOtherBranchCustomer() {
        /* Arrange */
        CreditApplication application = createApplication();
        stubApplication(application);
        when(customers.findByIdAndBranchIdAndActiveTrue(
                1L, 3L))
                .thenReturn(Optional.empty());

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.approve(application.getApplicationCode())
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND);
        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.PENDING);

        verifyApplicationLookup(application);
        verify(customers).findByIdAndBranchIdAndActiveTrue(1L, 3L);
        verifyNoInteractions(vehicles, users);
        verifyNoMoreInteractions(
                branches,
                applications,
                customers
        );
    }

    @Test
    void shouldRejectMissingDeletedOrOtherBranchVehicle() {
        /* Arrange */
        CreditApplication application = createApplication();
        stubApplication(application);
        when(customers.findByIdAndBranchIdAndActiveTrue(1L, 3L))
                .thenReturn(Optional.of(createCustomer()));
        when(vehicles.findByIdAndBranchIdForUpdate(2L, 3L))
                .thenReturn(Optional.empty());

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.approve(application.getApplicationCode())
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_NOT_FOUND);
        assertThat(application.getStatus()).isEqualTo(CreditApplicationStatus.PENDING);

        verifyEligibleLookups(application);
        verifyNoInteractions(users);
        verifyNoMoreInteractions(
                branches,
                applications,
                customers,
                vehicles
        );
    }

    @ParameterizedTest
    @EnumSource(value = VehicleStatus.class, names = {"AVAILABLE", "SOLD"})
    void shouldRejectVehicleWithoutReservation(VehicleStatus status) {
        /* Arrange */
        CreditApplication application = createApplication();
        Vehicle vehicle = createVehicle();
        ReflectionTestUtils.setField(vehicle, "status", status);
        stubEligible(application, vehicle);

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.approve(application.getApplicationCode())
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_OUT_OF_STOCK);
        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.PENDING);
        assertThat(vehicle.getStatus())
                .isEqualTo(status);

        verifyEligibleLookups(application);
        verifyNoInteractions(users);
        verifyNoMoreInteractions(
                branches,
                applications,
                customers,
                vehicles
        );
    }

    @Test
    void shouldRejectMissingResolverWithoutSaving() {
        /* Arrange */
        CreditApplication application = createApplication();
        stubEligible(application, createVehicle());
        when(users.getCurrentUserId()).thenReturn(null);

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.approve(application.getApplicationCode())
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREDIT_APPLICATION_RESOLVER_REQUIRED);
        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.PENDING);

        verifyEligibleLookups(application);
        verify(users).getCurrentUserId();
        verifyNoMoreInteractions(
                branches,
                applications,
                customers,
                vehicles,
                users
        );
    }

    private void stubApplication(CreditApplication application) {
        when(branches.getCurrentBranchId()).thenReturn(3L);
        when(applications.findByApplicationCodeAndBranchIdForUpdate(
                application.getApplicationCode(),
                3L))
                .thenReturn(Optional.of(application));
    }

    private void stubEligible(CreditApplication application, Vehicle vehicle) {
        stubApplication(application);
        when(customers.findByIdAndBranchIdAndActiveTrue(1L, 3L))
                .thenReturn(Optional.of(createCustomer()));
        when(vehicles.findByIdAndBranchIdForUpdate(2L, 3L))
                .thenReturn(Optional.of(vehicle));
    }

    private void verifyApplicationLookup(CreditApplication application) {
        verify(branches).getCurrentBranchId();
        verify(applications)
                .findByApplicationCodeAndBranchIdForUpdate(
                        application.getApplicationCode(),
                        3L);
    }

    private void verifyEligibleLookups(CreditApplication application) {
        verifyApplicationLookup(application);
        verify(customers).findByIdAndBranchIdAndActiveTrue(1L, 3L);
        verify(vehicles).findByIdAndBranchIdForUpdate(2L, 3L);
    }

    private UUID createApplicationCode() {
        return UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    }

    private CreditApplication createApplication() {
        return CreditApplication.create(
                createApplicationCode(),
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

    private Customer createCustomer() {
        CreateCustomerRequest request = createCustomerRequest();
        Customer customer = Customer.create(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440002"),
                request.fullName(),
                request.documentType(),
                request.documentNumber(),
                request.phone(),
                request.email(),
                request.occupation(),
                request.monthlyIncome(),
                request.address(),
                request.mapReference(),
                3L
        );
        /* Representa una entidad devuelta por persistencia; el request no admite id. */
        customer.setId(1L);
        return customer;
    }

    private Vehicle createVehicle() {
        CreateVehicleRequest request = createVehicleRequest();
        Vehicle vehicle = Vehicle.create(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440003"),
                request.vehicleType(),
                request.brand(),
                request.model(),
                request.manufacturingYear(),
                request.color(),
                request.chassisNumber(),
                request.engineNumber(),
                request.duaNumber(),
                request.referencePrice(),
                3L
        );
        vehicle.setId(2L);
        vehicle.reserveForFinancing();
        return vehicle;
    }

    private CreateCustomerRequest createCustomerRequest() {
        return new CreateCustomerRequest(
                "Cliente de prueba",
                CustomerDocumentType.DNI,
                "12345678",
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private CreateVehicleRequest createVehicleRequest() {
        return new CreateVehicleRequest(
                "MOTO",
                "Bajaj",
                "Pulsar NS200",
                2026,
                "Negro",
                "CHASSIS-001",
                "ENGINE-001",
                null,
                new BigDecimal("12500.00")
        );
    }

}
