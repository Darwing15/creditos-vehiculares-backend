package pe.sotomayor.creditos.credit.application.service.creditapplication;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.application.mapper.creditapplication.CreditApplicationMapper;

import pe.sotomayor.creditos.credit.application.usecase.creditapplication.ListCreditApplicationsUseCase;

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;

import pe.sotomayor.creditos.credit.domain.repository.CreditApplicationRepository;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import java.util.List;

/**
 * Implementa el caso de uso para listar solicitudes de crédito
 * según su estado.
 *
 * <p>
 * El servicio consulta las solicitudes correspondientes y transforma
 * cada agregado encontrado en su representación de respuesta.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListCreditApplicationsService
        implements ListCreditApplicationsUseCase {

    private final CurrentBranchProvider
            currentBranchProvider;

    private final CreditApplicationRepository
            creditApplicationRepository;

    private final CreditApplicationMapper
            creditApplicationMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CreditApplicationResponse> listByStatus(
            CreditApplicationStatus status) {

        return creditApplicationRepository
                .findAllByStatusAndBranchId(
                        status,
                        currentBranchProvider.getCurrentBranchId()
                )
                .stream()
                .map(creditApplicationMapper::toResponse)
                .toList();
    }

}
