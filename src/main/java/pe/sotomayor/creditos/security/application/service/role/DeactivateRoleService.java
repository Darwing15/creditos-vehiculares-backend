package pe.sotomayor.creditos.security.application.service.role;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;
import pe.sotomayor.creditos.security.application.mapper.role.RoleMapper;
import pe.sotomayor.creditos.security.application.usecase.role.DeactivateRoleUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import java.time.LocalDateTime;

/**
 * Implementa el caso de uso para desactivar roles existentes mediante
 * eliminación lógica.
 *
 * <p>
 * Este servicio valida que el rol exista y que no esté siendo utilizado
 * por usuarios antes de marcar el registro como eliminado lógicamente,
 * conservando su información histórica.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeactivateRoleService implements DeactivateRoleUseCase {

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private final RoleMapper roleMapper;

    /**
     * Desactiva lógicamente un rol existente.
     *
     * @param id identificador único del rol
     * @return información del rol desactivado
     */
    @Override
    public RoleResponse deactivate(Long id) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.ROLE_NOT_FOUND,
                                "Role with id '%s' not found."
                                        .formatted(id)));

        if (userRepository.existsByRoleId(id)) {

            throw new ConflictException(
                    ErrorCode.ROLE_IN_USE,
                    "Role with id '%s is currently assigned to users."
                            .formatted(id));
        }

        role.setDeleted(true);

        role.setDeletedAt(LocalDateTime.now());

        Role savedRole = roleRepository.save(role);

        return roleMapper.toResponse(savedRole);
    }
}
