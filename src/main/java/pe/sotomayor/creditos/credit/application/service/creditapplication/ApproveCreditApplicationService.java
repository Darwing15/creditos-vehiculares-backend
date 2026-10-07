package pe.sotomayor.creditos.credit.application.service.creditapplication;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.application.mapper.creditapplication.CreditApplicationMapper;

import pe.sotomayor.creditos.credit.application.usecase.creditapplication.ApproveCreditApplicationUseCase;

import pe.sotomayor.creditos.credit.domain.model.entity.CreditApplication;

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;
import pe.sotomayor.creditos.credit.domain.repository.CreditApplicationRepository;

import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.shared.security.CurrentUserProvider;

import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;
import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;
import pe.sotomayor.creditos.vehicles.domain.repository.VehicleRepository;

import java.time.Clock;
import java.time.OffsetDateTime;

import java.util.UUID;

/**
 * Aprueba una solicitud pendiente de la sucursal autenticada.
 * Revalida cliente y reserva antes de registrar la decision. Conserva las
 * condiciones financieras fijadas al crear la solicitud y la unidad RESERVED.
 * La autorizacion HTTP credit-applications:resolve pertenece a SecurityConfig.
 * No crea Credit, contrato, cuotas, pagos ni movimientos de caja.
 *
 * @author DarwinG
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ApproveCreditApplicationService
        implements ApproveCreditApplicationUseCase {

    private final CurrentBranchProvider
            currentBranchProvider;

    private final CreditApplicationRepository
            creditApplicationRepository;

    private final CreditApplicationMapper
            creditApplicationMapper;

    private final CurrentUserProvider
            currentUserProvider;

    private final Clock clock;

    private final CustomerRepository
            customerRepository;

    private final VehicleRepository
            vehicleRepository;

    /**
     * {@inheritDoc}
     * Bloquea primero la solicitud y luego la unidad, igual que rechazo/cancelacion.
     * Una segunda resolucion de la misma solicitud se rechaza con HTTP 409.
     */
    @Override
    public CreditApplicationResponse approve(
            UUID applicationCode) {

        Long branchId = currentBranchProvider.getCurrentBranchId();

        CreditApplication application =
                findApplication(applicationCode, branchId);

        approveApplication(application, branchId);

        CreditApplication savedApplication =
                creditApplicationRepository.save(application);

        return creditApplicationMapper.toResponse(
                savedApplication
        );
    }

    /**
     * Obtiene la solicitud identificada por su codigo publico.
     *
     * @param applicationCode codigo publico de la solicitud.
     * @return solicitud encontrada.
     * @throws ResourceNotFoundException si la solicitud no existe.
     */
    private CreditApplication findApplication(
            UUID applicationCode, Long branchId) {

        return creditApplicationRepository
                .findByApplicationCodeAndBranchIdForUpdate(
                        applicationCode,
                        branchId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.CREDIT_APPLICATION_NOT_FOUND,
                                "Credit application not found."
                        )
                );
    }

    /**
     * Delega al agregado la aprobacion de la solicitud utilizando
     * la identidad del usuario autenticado y el tiempo actual.
     *
     * @param application solicitud pendiente de aprobacion.
     */
    private void approveApplication(
            CreditApplication application, Long branchId) {

        OffsetDateTime resolvedAt =
                OffsetDateTime.now(clock);

        /*
         * Evita consultar recursos de una solicitud que ya termino su evaluacion.
         * El agregado vuelve a validar la transicion al ejecutar approve.
         */
        if (!application.getStatus().canTransitionTo(
                CreditApplicationStatus.APPROVED)) {
            throw new ConflictException(
                    ErrorCode.CREDIT_APPLICATION_INVALID_STATUS_TRANSITION,
                    "Credit application cannot transition from "
                            + application.getStatus() + " to APPROVED");
        }

        validateEligibility(application, branchId);

        Long currentUserId =
                currentUserProvider.getCurrentUserId();

        application.approve(
                resolvedAt,
                currentUserId
        );
    }

    /**
     * Comprueba que el cliente siga activo y que la unidad conserve su reserva.
     * No compara el precio historico con un nuevo precio de catalogo ni recalcula tasas.
     *
     * @param application solicitud pendiente bloqueada
     * @param branchId    sucursal obtenida del principal
     */
    private void validateEligibility(
            CreditApplication application, Long branchId) {
        customerRepository.findByIdAndBranchIdAndActiveTrue(
                        application.getCustomerId(), branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND,
                        "Active customer was not found in the authenticated branch"));

        Vehicle vehicle = vehicleRepository
                .findByIdAndBranchIdForUpdate(
                        application.getVehicleId(), branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND,
                        "Reserved vehicle was not found in the authenticated branch"));

        if (vehicle.getStatus() != VehicleStatus.RESERVED) {
            throw new ConflictException(ErrorCode.VEHICLE_OUT_OF_STOCK,
                    "Vehicle must retain its financing reservation before approval");
        }
    }

}
