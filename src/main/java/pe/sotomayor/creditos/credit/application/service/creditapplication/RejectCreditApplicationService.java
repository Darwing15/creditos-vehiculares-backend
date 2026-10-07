package pe.sotomayor.creditos.credit.application.service.creditapplication;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.request.RejectCreditApplicationRequest;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.application.mapper.creditapplication.CreditApplicationMapper;

import pe.sotomayor.creditos.credit.application.usecase.creditapplication.RejectCreditApplicationUseCase;

import pe.sotomayor.creditos.credit.domain.model.entity.CreditApplication;

import pe.sotomayor.creditos.credit.domain.repository.CreditApplicationRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.shared.security.CurrentUserProvider;

import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;
import pe.sotomayor.creditos.vehicles.domain.repository.VehicleRepository;

import java.time.Clock;
import java.time.OffsetDateTime;

import java.util.UUID;

/**
 * Implementa el caso de uso para rechazar solicitudes de crédito.
 *
 * <p>
 * El servicio localiza la solicitud, obtiene la identidad del usuario
 * responsable y el instante actual, delega la transición de estado
 * al agregado y persiste el resultado.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RejectCreditApplicationService
        implements RejectCreditApplicationUseCase {

    private final CurrentBranchProvider
            currentBranchProvider;

    private final CreditApplicationRepository
            creditApplicationRepository;

    private final CreditApplicationMapper
            creditApplicationMapper;

    private final CurrentUserProvider
            currentUserProvider;

    private final Clock clock;

    private final VehicleRepository
            vehicleRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public CreditApplicationResponse reject(
            UUID applicationCode,
            RejectCreditApplicationRequest request) {

        CreditApplication application =
                findApplication(applicationCode);

        rejectApplication(
                application,
                request.rejectionReason()
        );

        releaseReservation(application);

        CreditApplication savedApplication =
                creditApplicationRepository.save(application);

        return creditApplicationMapper.toResponse(
                savedApplication
        );
    }

    /**
     * Obtiene la solicitud identificada por su código público.
     *
     * @param applicationCode código público de la solicitud.
     * @return solicitud encontrada.
     * @throws ResourceNotFoundException si la solicitud no existe.
     */
    private CreditApplication findApplication(
            UUID applicationCode) {

        return creditApplicationRepository
                .findByApplicationCodeAndBranchIdForUpdate(
                        applicationCode,
                        currentBranchProvider.getCurrentBranchId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.CREDIT_APPLICATION_NOT_FOUND,
                                "Credit application not found."
                        )
                );
    }

    /**
     * Delega al agregado el rechazo de la solicitud utilizando
     * la identidad del usuario autenticado, el tiempo actual
     * y el motivo proporcionado.
     *
     * @param application     solicitud pendiente de rechazo.
     * @param rejectionReason motivo del rechazo.
     */
    private void rejectApplication(
            CreditApplication application,
            String rejectionReason) {

        Long currentUserId =
                currentUserProvider.getCurrentUserId();

        OffsetDateTime resolvedAt =
                OffsetDateTime.now(clock);

        application.reject(
                resolvedAt,
                currentUserId,
                rejectionReason
        );
    }

    /**
     * Libera la unidad dentro de la misma transacción que resuelve la solicitud.
     * El orden de bloqueo es solicitud y luego vehículo.
     *
     * @param application solicitud ya validada por el agregado
     */
    private void releaseReservation(CreditApplication application) {
        Vehicle vehicle = vehicleRepository.findByIdAndBranchIdForUpdate(
                        application.getVehicleId(),
                        application.getBranchId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND,
                        "Reserved vehicle was not found")
                );
        vehicle.releaseFinancingReservation();
        vehicleRepository.save(vehicle);
    }

}
