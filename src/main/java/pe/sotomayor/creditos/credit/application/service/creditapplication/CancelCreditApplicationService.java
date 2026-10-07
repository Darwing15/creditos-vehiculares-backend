package pe.sotomayor.creditos.credit.application.service.creditapplication;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.application.mapper.creditapplication.CreditApplicationMapper;

import pe.sotomayor.creditos.credit.application.usecase.creditapplication.CancelCreditApplicationUseCase;

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
 * Implementa el caso de uso para cancelar solicitudes de crédito.
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
public class CancelCreditApplicationService
        implements CancelCreditApplicationUseCase {

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
    public CreditApplicationResponse cancel(
            UUID applicationCode) {

        CreditApplication application =
                findApplication(applicationCode);

        cancelApplication(application);

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
     * Delega al agregado la cancelación de la solicitud utilizando
     * la identidad del usuario autenticado y el tiempo actual.
     *
     * @param application solicitud pendiente de cancelación.
     */
    private void cancelApplication(
            CreditApplication application) {

        Long currentUserId =
                currentUserProvider.getCurrentUserId();

        OffsetDateTime resolvedAt =
                OffsetDateTime.now(clock);

        application.cancel(
                resolvedAt,
                currentUserId
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
