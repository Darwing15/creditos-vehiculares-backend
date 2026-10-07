package pe.sotomayor.creditos.credit.application.service.creditapplication;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.application.mapper.creditapplication.CreditApplicationMapper;

import pe.sotomayor.creditos.credit.application.usecase.creditapplication.GetCreditApplicationUseCase;

import pe.sotomayor.creditos.credit.domain.model.entity.CreditApplication;

import pe.sotomayor.creditos.credit.domain.repository.CreditApplicationRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import java.util.UUID;

/**
 * Implementa el caso de uso para consultar solicitudes de crédito
 * mediante su identificador público.
 *
 * <p>
 * El servicio localiza la solicitud y transforma el agregado
 * encontrado en su representación de respuesta.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetCreditApplicationService
        implements GetCreditApplicationUseCase {

    private final CreditApplicationRepository
            creditApplicationRepository;

    private final CreditApplicationMapper
            creditApplicationMapper;

    private final CurrentBranchProvider
            currentBranchProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public CreditApplicationResponse getByApplicationCode(
            UUID applicationCode) {

        CreditApplication application =
                findApplication(applicationCode);

        return creditApplicationMapper.toResponse(
                application
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
                .findByApplicationCodeAndBranchId(
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

}
