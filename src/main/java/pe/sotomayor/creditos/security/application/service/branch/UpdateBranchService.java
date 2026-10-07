package pe.sotomayor.creditos.security.application.service.branch;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.branch.request.UpdateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

import pe.sotomayor.creditos.security.application.mapper.branch.BranchMapper;

import pe.sotomayor.creditos.security.application.usecase.branch.UpdateBranchUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;

import pe.sotomayor.creditos.security.domain.repository.BranchRepository;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;
import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.util.Objects;

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
@Transactional(isolation = Isolation.READ_COMMITTED)
@RequiredArgsConstructor
public class UpdateBranchService implements UpdateBranchUseCase {

    private final BranchRepository branchRepository;

    private final UserRepository userRepository;

    private final BranchMapper branchMapper;

    private final SecurityWriteGate securityWriteGate;

    /**
     * Actualiza una sucursal existente.
     *
     * @param id      identificador único de la sucursal
     * @param request información administrativa actualizada
     * @return información de la sucursal actualizada
     */
    @Override
    @PreAuthorize("hasAuthority('security:branches:write')")
    public BranchResponse update(Long id, UpdateBranchRequest request) {

        securityWriteGate.begin(SecurityPermission.BRANCHES_WRITE);

        Branch branch =
                branchRepository.findByIdAndDeletedFalse(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        ErrorCode.BRANCH_NOT_FOUND,
                                        "Branch with id '%s' not found."
                                                .formatted(id)
                                ));

        if (request.managerId() != null) {
            /* manager se obtiene de la base de datos para validar estado y pertenencia;
             managerId por sí solo solo acredita el identificador enviado en el formulario. */
            User manager = userRepository.findByIdAndDeletedFalse(request.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorCode.USER_NOT_FOUND, "No existe el responsable indicado."));
            if (!Boolean.TRUE.equals(manager.getActive())
                    || !Objects.equals(manager.getBranch().getId(), branch.getId())) {
                throw new ValidationException(
                        ErrorCode.VALIDATION_ERROR,
                        "El responsable debe ser un usuario activo de esta sucursal.");
            }
        }

        branchMapper.updateEntity(request, branch);

        Branch updatedBranch = branchRepository.saveAndFlush(branch);
        securityWriteGate.finish();

        return branchMapper.toResponse(updatedBranch);
    }

}
