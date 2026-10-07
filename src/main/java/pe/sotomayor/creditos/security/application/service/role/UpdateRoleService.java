package pe.sotomayor.creditos.security.application.service.role;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

import pe.sotomayor.creditos.security.application.mapper.role.RoleMapper;
import pe.sotomayor.creditos.security.application.usecase.role.UpdateRoleUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;

import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

/**
 * Implementa el caso de uso para actualizar roles existentes.
 *
 * <p>
 * Este servicio valida la existencia del rol y evita conflictos
 * de nombres duplicados antes de aplicar modificaciones sobre
 * una entidad existente.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
@RequiredArgsConstructor
public class UpdateRoleService implements UpdateRoleUseCase {

    private final RoleRepository roleRepository;

    private final RoleMapper roleMapper;

    private final SecurityWriteGate securityWriteGate;

    /**
     * Actualiza la información administrativa de un rol existente.
     *
     * <p>
     * La identidad del rol se mantiene y únicamente se modifican
     * los atributos permitidos definidos por el caso de uso.
     * </p>
     *
     * @param id      identificador único del rol
     * @param request información actualizada del rol
     * @return información del rol actualizado
     */
    @Override
    @PreAuthorize("hasAuthority('security:roles:write')")
    public RoleResponse update(Long id, UpdateRoleRequest request) {

        var actor = securityWriteGate.begin(SecurityPermission.ROLES_WRITE);

        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROLE_NOT_FOUND,
                        "Role with id '%s' not found.".formatted(id)
                ));

        actor.requireSubset(role.getPermissions());

        if (roleRepository.existsByNameAndIdNot(
                request.name(), id)) {

            throw new ConflictException(
                    ErrorCode.ROLE_ALREADY_EXISTS,
                    "Role with name '%s' already exists."
                            .formatted(request.name()));
        }

        roleMapper.updateEntity(request, role);

        Role updatedRole = roleRepository.saveAndFlush(role);
        securityWriteGate.finish();

        return roleMapper.toResponse(updatedRole);
    }
}
