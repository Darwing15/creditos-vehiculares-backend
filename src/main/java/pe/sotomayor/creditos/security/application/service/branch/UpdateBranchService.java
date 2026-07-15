package pe.sotomayor.creditos.security.application.service.branch;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.branch.request.UpdateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

import pe.sotomayor.creditos.security.application.mapper.branch.BranchMapper;
import pe.sotomayor.creditos.security.application.usecase.branch.UpdateBranchUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.repository.BranchRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

/**
 * Implementa el caso de uso para actualizar sucursales existentes.
 *
 * <p>
 * Este servicio permite modificar la información administrativa
 * de una sucursal manteniendo su identidad operativa.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateBranchService implements UpdateBranchUseCase {

    private final BranchRepository branchRepository;

    private final BranchMapper branchMapper;

    /**
     * Actualiza una sucursal existente.
     *
     * @param id      identificador único de la sucursal
     * @param request información administrativa actualizada
     * @return información de la sucursal actualizada
     */
    @Override
    public BranchResponse update(Long id, UpdateBranchRequest request) {

        Branch branch =
                branchRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        ErrorCode.BRANCH_NOT_FOUND,
                                        "Branch with id '%s' not found."
                                                .formatted(id)
                                ));

        branchMapper.updateEntity(request, branch);

        Branch updatedBranch = branchRepository.save(branch);

        return branchMapper.toResponse(updatedBranch);
    }

}
