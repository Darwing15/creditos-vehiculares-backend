package pe.sotomayor.creditos.credit.application.service.creditapplication;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.request.CreateCreditApplicationRequest;
import pe.sotomayor.creditos.credit.application.mapper.creditapplication.CreditApplicationMapper;

import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;
import pe.sotomayor.creditos.credit.domain.repository.CreditApplicationRepository;
import pe.sotomayor.creditos.credit.domain.service.CreditCalculator;
import pe.sotomayor.creditos.credit.domain.service.creditapplication.CreditApplicationCodeGenerator;
import pe.sotomayor.creditos.credit.domain.service.creditapplication.CreditApplicationNumberGenerator;
import pe.sotomayor.creditos.credit.domain.service.installment.DefaultInstallmentCalculator;
import pe.sotomayor.creditos.credit.domain.service.installment.InstallmentTermsCalculator;

import pe.sotomayor.creditos.customers.domain.model.entity.Customer;
import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;

import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;
import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;
import pe.sotomayor.creditos.vehicles.domain.repository.VehicleRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCreditApplicationEligibilityTest {

    @Mock
    CreditApplicationRepository applications;

    @Mock
    CreditApplicationMapper mapper;

    @Mock
    CreditCalculator calculator;

    @Mock
    InstallmentTermsCalculator termsCalculator;

    @Mock
    CreditApplicationCodeGenerator codes;

    @Mock
    CreditApplicationNumberGenerator numbers;

    @Mock
    CustomerRepository customers;

    @Mock
    VehicleRepository vehicles;

    @Mock
    CurrentBranchProvider branch;

    @InjectMocks
    CreateCreditApplicationService service;

    @BeforeEach
    void authenticatedBranch() {
        when(branch.getCurrentBranchId())
                .thenReturn(3L);
    }

    @Test
    void absentOrInactiveCustomerInBranchDoesNotReserve() {

        /* Arrange */

        when(customers.findByIdAndBranchIdAndActiveTrue(1L, 3L))
                .thenReturn(Optional.empty());

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.create(request())
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND);

        verifyNoInteractions(
                vehicles,
                applications,
                mapper,
                calculator,
                codes,
                numbers
        );
    }

    @Test
    void absentVehicleInBranchDoesNotWrite() {

        /* Arrange */

        activeCustomer();

        when(vehicles.findByIdAndBranchIdForUpdate(2L, 3L))
                .thenReturn(Optional.empty());

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.create(request())
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_NOT_FOUND);

        verifyNoInteractions(applications, calculator);

        verify(vehicles, never())
                .save(any());
    }

    @Test
    void alreadyReservedUnitCannotBeReservedTwice() {

        /* Arrange */

        Vehicle vehicle = vehicle(VehicleStatus.RESERVED, "12000.00");

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.create(request())
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_OUT_OF_STOCK);

        assertThat(vehicle.getStatus())
                .isEqualTo(VehicleStatus.RESERVED);

        verify(vehicles, never()).save(any());

        verifyNoInteractions(applications, calculator);
    }

    @Test
    void changedPriceRequiresReselectingInventory() {

        /* Arrange */

        Vehicle vehicle = vehicle(VehicleStatus.AVAILABLE, "12500.00");

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.create(request())
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class);

        assertThat(vehicle.getStatus())
                .isEqualTo(VehicleStatus.AVAILABLE);

        verify(vehicles, never())
                .save(any());

        verifyNoInteractions(applications, calculator);
    }

    @Test
    void invalidFinancingDoesNotReserve() {

        /* Arrange */

        Vehicle vehicle = vehicle(VehicleStatus.AVAILABLE, "12000.00");

        when(calculator.calculateFinancedAmount(
                new BigDecimal("12000.00"),
                new BigDecimal("2000.00")))
                .thenThrow(new ValidationException(
                        ErrorCode.CREDIT_APPLICATION_INVALID_DOWN_PAYMENT,
                        "Invalid down payment"));

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.create(request())
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class);

        assertThat(vehicle.getStatus())
                .isEqualTo(VehicleStatus.AVAILABLE);

        verify(vehicles, never())
                .save(any());

        verifyNoInteractions(applications, mapper, codes, numbers);
    }

    @Test
    void unfeasibleScheduleDoesNotReserveVehicle() {

        /* Arrange */

        Vehicle vehicle = vehicle(VehicleStatus.AVAILABLE, "12000.00");
        BigDecimal financed = new BigDecimal("1.00");
        BigDecimal rate = new BigDecimal("0.00");
        var request = new CreateCreditApplicationRequest(1L, 2L, FinancingCurrency.PEN,
                new BigDecimal("12000.00"), new BigDecimal("11999.00"), 2);
        when(calculator.calculateFinancedAmount(new BigDecimal("12000.00"),
                new BigDecimal("11999.00"))).thenReturn(financed);
        when(calculator.calculateCompensatoryInterestRate(financed, 2)).thenReturn(rate);
        var realTerms = new InstallmentTermsCalculator(new DefaultInstallmentCalculator());
        when(termsCalculator.calculate(financed, 2, rate))
                .thenAnswer(invocation -> realTerms.calculate(financed, 2, rate));

        /* Act */

        Throwable thrown = catchThrowable(() -> service.create(request));

        /* Assert */

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CREDIT_APPLICATION_UNFEASIBLE_INSTALLMENTS);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        verify(vehicles, never()).save(any());
        verifyNoInteractions(applications, mapper, codes, numbers);
    }

    private void activeCustomer() {
        when(customers.findByIdAndBranchIdAndActiveTrue(1L, 3L))
                .thenReturn(Optional.of(mock(Customer.class)));
    }

    private Vehicle vehicle(VehicleStatus status, String price) {
        activeCustomer();
        Vehicle vehicle = BeanUtils.instantiateClass(Vehicle.class);
        ReflectionTestUtils.setField(vehicle, "status", status);
        ReflectionTestUtils.setField(vehicle, "referencePrice", new BigDecimal(price));
        when(vehicles.findByIdAndBranchIdForUpdate(2L, 3L))
                .thenReturn(Optional.of(vehicle));
        return vehicle;
    }

    private CreateCreditApplicationRequest request() {
        return new CreateCreditApplicationRequest(
                1L,
                2L,
                FinancingCurrency.PEN,
                new BigDecimal("12000.00"),
                new BigDecimal("2000.00"),
                12
        );
    }
}

