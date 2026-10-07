package pe.sotomayor.creditos.security.application.service.branch;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.branch.request.CreateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

import pe.sotomayor.creditos.security.application.mapper.branch.BranchMapper;
import pe.sotomayor.creditos.security.application.usecase.branch.CreateBranchUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;
import pe.sotomayor.creditos.security.domain.repository.BranchRepository;

import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

/**
 * Implementa el caso de uso para registrar nuevas sucursales.
 *
 * <p>
 * Este servicio valida las reglas de negocio necesarias para crear
 * una sucursal, incluyendo la unicidad del código y la asignación
 * del estado inicial de disponibilidad.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
@RequiredArgsConstructor
public class CreateBranchService implements CreateBranchUseCase {

    private final BranchRepository branchRepository;

    private final BranchMapper branchMapper;

    private final SecurityWriteGate securityWriteGate;

    /**
     * Registra una nueva sucursal.
     *
     * @param request información requerida para crear la sucursal
     * @return información de la sucursal creada
     */
    @Override
    @PreAuthorize("hasAuthority('security:branches:write')")
    public BranchResponse create(CreateBranchRequest request) {

        securityWriteGate.begin(SecurityPermission.BRANCHES_WRITE);

        if (branchRepository.existsByCode(request.code())) {

            throw new ConflictException(
                    ErrorCode.BRANCH_ALREADY_EXISTS,
                    "Branch with code '%s' already exists."
                            .formatted(request.code())
            );
        }

        if (request.managerId() != null) {
            throw new ValidationException(
                    ErrorCode.VALIDATION_ERROR,
                    "Cree la sucursal sin responsable y asígnelo después de registrar sus usuarios."
            );
        }

        Branch branch = branchMapper.toEntity(request);

        branch.setActive(true);

        Branch savedBranch = branchRepository.saveAndFlush(branch);
        securityWriteGate.finish();

        return branchMapper.toResponse(savedBranch);
    }

}
