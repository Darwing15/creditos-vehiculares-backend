package pe.sotomayor.creditos.credit.application.service.creditapplication;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.request.CreateCreditApplicationRequest;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.application.mapper.creditapplication.CreditApplicationMapper;

import pe.sotomayor.creditos.credit.application.usecase.creditapplication.CreateCreditApplicationUseCase;

import pe.sotomayor.creditos.credit.domain.model.entity.CreditApplication;

import pe.sotomayor.creditos.credit.domain.repository.CreditApplicationRepository;

import pe.sotomayor.creditos.credit.domain.service.CreditCalculator;

import pe.sotomayor.creditos.credit.domain.service.creditapplication.CreditApplicationCodeGenerator;
import pe.sotomayor.creditos.credit.domain.service.creditapplication.CreditApplicationNumberGenerator;

import pe.sotomayor.creditos.credit.domain.service.installment.InstallmentTermsCalculator;
import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;
import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;

import pe.sotomayor.creditos.vehicles.domain.repository.VehicleRepository;

import java.math.BigDecimal;

/**
 * Registra una solicitud tras validar cliente, sucursal e inventario.
 * La reserva y la solicitud participan en la misma transaccion.
 *
 * @author DarwinG
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CreateCreditApplicationService
        implements CreateCreditApplicationUseCase {

    private final CreditApplicationRepository
            creditApplicationRepository;

    private final CreditApplicationMapper
            creditApplicationMapper;

    private final CreditCalculator
            creditCalculator;

    private final InstallmentTermsCalculator
            installmentTermsCalculator;

    private final CreditApplicationCodeGenerator
            creditApplicationCodeGenerator;

    private final CreditApplicationNumberGenerator
            creditApplicationNumberGenerator;

    private final CustomerRepository
            customerRepository;

    private final VehicleRepository
            vehicleRepository;

    private final CurrentBranchProvider
            currentBranchProvider;


    /**
     * {@inheritDoc}
     */
    @Override
    public CreditApplicationResponse create(
            CreateCreditApplicationRequest request) {

        CreditApplication application =
                createApplication(request);

        CreditApplication saved =
                creditApplicationRepository.save(application);

        return creditApplicationMapper.toResponse(
                saved
        );
    }

    /**
     * Crea una nueva solicitud de credito a partir de los datos
     * proporcionados y de la informacion administrada por el sistema.
     *
     * @param request informacion proporcionada para registrar
     *                la solicitud.
     * @return nueva solicitud de credito pendiente.
     */
    private CreditApplication createApplication(
            CreateCreditApplicationRequest request) {

        Long branchId = currentBranchProvider.getCurrentBranchId();

        customerRepository.findByIdAndBranchIdAndActiveTrue(request.customerId(), branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND,
                        "Active customer was not found in the authenticated branch")
                );

        Vehicle vehicle = vehicleRepository
                .findByIdAndBranchIdForUpdate(request.vehicleId(), branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND,
                        "Vehicle was not found in the authenticated branch")
                );

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new ConflictException(ErrorCode.VEHICLE_OUT_OF_STOCK,
                    "Vehicle is no longer available for financing");
        }

        if (request.vehiclePrice().compareTo(vehicle.getReferencePrice()) != 0) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR,
                    "Vehicle price must match the current inventory reference price");
        }

        BigDecimal price = vehicle.getReferencePrice();

        BigDecimal financed =
                creditCalculator.calculateFinancedAmount(
                        price,
                        request.downPayment()
                );

        BigDecimal rate =
                creditCalculator.calculateCompensatoryInterestRate(
                        financed,
                        request.installmentCount()
                );

        installmentTermsCalculator
                .calculate(financed, request.installmentCount(), rate);

        vehicle.reserveForFinancing();
        vehicleRepository.save(vehicle);

        return CreditApplication.create(
                creditApplicationCodeGenerator.generate(),
                creditApplicationNumberGenerator.generate(),
                request.currency(),
                price,
                request.downPayment(),
                financed,
                rate,
                request.installmentCount(),
                request.customerId(),
                request.vehicleId(),
                branchId
        );
    }

}
