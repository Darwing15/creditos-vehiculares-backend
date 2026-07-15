package pe.sotomayor.creditos.security.application.service.branch;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

import pe.sotomayor.creditos.security.application.mapper.branch.BranchMapper;

import pe.sotomayor.creditos.security.application.usecase.branch.DeactivateBranchUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;

import pe.sotomayor.creditos.security.domain.repository.BranchRepository;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

/**
 * Implementa el caso de uso para desactivar sucursales existentes.
 *
 * <p>
 * Gestiona el cambio de estado operativo de una sucursal,
 * evitando eliminar físicamente información histórica.
 * </p>
 *
 * <p>
 * Antes de realizar la desactivación valida que la sucursal exista
 * y que no tenga usuarios asociados activos dentro del sistema.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeactivateBranchService implements DeactivateBranchUseCase {

    private final BranchRepository branchRepository;

    private final UserRepository userRepository;

    private final BranchMapper branchMapper;

    /**
     * Desactiva una sucursal existente.
     *
     * @param id identificador único de la sucursal
     * @return información de la sucursal desactivada
     * @throws ResourceNotFoundException si la sucursal no existe
     * @throws ConflictException         si la sucursal tiene usuarios asociados
     */
    @Override
    public BranchResponse deactivate(Long id) {

        Branch branch = branchRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.BRANCH_NOT_FOUND,
                                "Branch with id '%s' not found."
                                        .formatted(id)
                        ));

        if (userRepository.existsByBranchId(id)) {

            throw new ConflictException(
                    ErrorCode.BRANCH_IN_USE,
                    "Branch with id '%s' has assigned users."
                            .formatted(id)
            );
        }

        branch.setActive(false);

        Branch savedBranch = branchRepository.save(branch);

        return branchMapper.toResponse(savedBranch);
    }

}
