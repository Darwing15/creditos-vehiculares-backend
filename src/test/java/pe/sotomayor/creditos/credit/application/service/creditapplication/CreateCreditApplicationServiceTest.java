package pe.sotomayor.creditos.credit.application.service.creditapplication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.request.CreateCreditApplicationRequest;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.application.mapper.creditapplication.CreditApplicationMapper;

import pe.sotomayor.creditos.credit.domain.model.entity.CreditApplication;
import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;
import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import pe.sotomayor.creditos.credit.domain.repository.CreditApplicationRepository;

import pe.sotomayor.creditos.credit.domain.service.CreditCalculator;

import pe.sotomayor.creditos.credit.domain.service.creditapplication.CreditApplicationCodeGenerator;

import pe.sotomayor.creditos.credit.domain.service.creditapplication.CreditApplicationNumberGenerator;

import pe.sotomayor.creditos.customers.domain.model.entity.Customer;
import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;
import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;

import pe.sotomayor.creditos.vehicles.domain.repository.VehicleRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCreditApplicationServiceTest {

    private static final Long CURRENT_BRANCH_ID = 3L;

    @Mock
    private CreditApplicationRepository
            creditApplicationRepository;

    @Mock
    private CreditApplicationMapper
            creditApplicationMapper;

    @Mock
    private CreditCalculator
            creditCalculator;

    @Mock
    private CreditApplicationCodeGenerator
            creditApplicationCodeGenerator;

    @Mock
    private CreditApplicationNumberGenerator
            creditApplicationNumberGenerator;

    @Mock
    private CurrentBranchProvider
            currentBranchProvider;

    @Mock
    private CustomerRepository
            customerRepository;

    @Mock
    private VehicleRepository
            vehicleRepository;

    @InjectMocks
    private CreateCreditApplicationService
            service;

    @Test
    void shouldCreateCreditApplicationWithSystemManagedData() {

        /* Arrange */

        CreateCreditApplicationRequest request =
                createValidRequest();

        UUID applicationCode =
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                );

        String applicationNumber =
                "CA-00000001";

        BigDecimal financedAmount =
                new BigDecimal("10000.00");

        BigDecimal compensatoryInterestRate =
                new BigDecimal("1.50");

        CreditApplicationResponse response =
                createResponse(
                        applicationCode,
                        applicationNumber,
                        financedAmount,
                        compensatoryInterestRate
                );

        when(creditCalculator.calculateFinancedAmount(
                request.vehiclePrice(),
                request.downPayment()
        )).thenReturn(financedAmount);

        when(creditCalculator.calculateCompensatoryInterestRate(
                financedAmount,
                request.installmentCount()
        )).thenReturn(compensatoryInterestRate);

        when(creditApplicationCodeGenerator.generate())
                .thenReturn(applicationCode);

        when(creditApplicationNumberGenerator.generate())
                .thenReturn(applicationNumber);

        when(creditApplicationRepository.save(
                any(CreditApplication.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        when(creditApplicationMapper.toResponse(
                any(CreditApplication.class)
        )).thenReturn(response);

        when(currentBranchProvider.getCurrentBranchId())
                .thenReturn(CURRENT_BRANCH_ID);

        when(customerRepository.findByIdAndBranchIdAndActiveTrue(1L, CURRENT_BRANCH_ID))
                .thenReturn(Optional.of(mock(Customer.class)));

        Vehicle vehicle = BeanUtils.instantiateClass(Vehicle.class);
        ReflectionTestUtils.setField(vehicle, "referencePrice", request.vehiclePrice());
        ReflectionTestUtils.setField(vehicle, "status", VehicleStatus.AVAILABLE);

        when(vehicleRepository.findByIdAndBranchIdForUpdate(2L, CURRENT_BRANCH_ID))
                .thenReturn(Optional.of(vehicle));

        /* Act */

        CreditApplicationResponse result =
                service.create(request);

        /* Assert */

        assertThat(result)
                .isSameAs(response);

        ArgumentCaptor<CreditApplication> captor =
                ArgumentCaptor.forClass(
                        CreditApplication.class
                );

        verify(creditApplicationRepository)
                .save(captor.capture());

        CreditApplication application =
                captor.getValue();

        assertThat(application.getApplicationCode())
                .isEqualTo(applicationCode);

        assertThat(application.getApplicationNumber())
                .isEqualTo(applicationNumber);

        assertThat(application.getCustomerId())
                .isEqualTo(request.customerId());

        assertThat(application.getVehicleId())
                .isEqualTo(request.vehicleId());

        assertThat(currentBranchProvider.getCurrentBranchId())
                .isEqualTo(CURRENT_BRANCH_ID);

        assertThat(application.getCurrency())
                .isEqualTo(request.currency());

        assertThat(application.getVehiclePrice())
                .isEqualByComparingTo(
                        request.vehiclePrice()
                );

        assertThat(application.getDownPayment())
                .isEqualByComparingTo(
                        request.downPayment()
                );

        assertThat(application.getFinancedAmount())
                .isEqualByComparingTo(financedAmount);

        assertThat(application.getCompensatoryInterestRate())
                .isEqualByComparingTo(
                        compensatoryInterestRate
                );

        assertThat(application.getInstallmentCount())
                .isEqualTo(request.installmentCount());

        assertThat(application.getStatus())
                .isEqualTo(
                        CreditApplicationStatus.PENDING
                );

        assertThat(application.getResolvedAt())
                .isNull();

        assertThat(application.getResolvedBy())
                .isNull();

        assertThat(application.getRejectionReason())
                .isNull();

        assertThat(vehicle.getStatus())
                .isEqualTo(VehicleStatus.RESERVED);

        verify(vehicleRepository)
                .save(vehicle);

        verify(creditApplicationMapper)
                .toResponse(application);
    }

    private CreateCreditApplicationRequest createValidRequest() {

        return new CreateCreditApplicationRequest(
                1L,
                2L,
                FinancingCurrency.PEN,
                new BigDecimal("12000.00"),
                new BigDecimal("2000.00"),
                12
        );
    }

    private CreditApplicationResponse createResponse(
            UUID applicationCode,
            String applicationNumber,
            BigDecimal financedAmount,
            BigDecimal compensatoryInterestRate) {

        return new CreditApplicationResponse(
                applicationCode,
                applicationNumber,
                1L,
                2L,
                3L,
                FinancingCurrency.PEN,
                new BigDecimal("12000.00"),
                new BigDecimal("2000.00"),
                financedAmount,
                compensatoryInterestRate,
                12,
                CreditApplicationStatus.PENDING,
                null,
                null,
                null
        );
    }

}
