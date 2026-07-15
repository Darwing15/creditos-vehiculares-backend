package pe.sotomayor.creditos.security.application.service.role;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

import pe.sotomayor.creditos.security.application.mapper.role.RoleMapper;
import pe.sotomayor.creditos.security.application.usecase.role.UpdateRoleUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;

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
@Transactional
@RequiredArgsConstructor
public class UpdateRoleService implements UpdateRoleUseCase {

    private final RoleRepository roleRepository;

    private final RoleMapper roleMapper;

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
    public RoleResponse update(Long id, UpdateRoleRequest request) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROLE_NOT_FOUND,
                        "Role with id '%s' not found.".formatted(id)
                ));

        if (roleRepository.existsByNameAndIdNot(
                request.name(), id)) {

            throw new ConflictException(
                    ErrorCode.ROLE_ALREADY_EXISTS,
                    "Role with name '%s' already exists."
                            .formatted(request.name()));
        }

        roleMapper.updateEntity(request, role);

        Role updatedRole = roleRepository.save(role);

        return roleMapper.toResponse(updatedRole);
    }
}
