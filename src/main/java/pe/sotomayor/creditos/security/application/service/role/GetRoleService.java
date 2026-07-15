package pe.sotomayor.creditos.security.application.service.role;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;
import pe.sotomayor.creditos.security.application.usecase.role.GetRoleByIdUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;

import pe.sotomayor.creditos.security.application.mapper.role.RoleMapper;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

/**
 * Implementa el caso de uso para obtener un rol mediante su identificador.
 *
 * <p>
 * Este servicio coordina la consulta de un rol, delegando el acceso a datos al
 * repositorio y la conversión de entidades al mapper correspondiente.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetRoleService implements GetRoleByIdUseCase {

    private final RoleRepository roleRepository;

    private final RoleMapper roleMapper;

    /**
     * Obtiene un rol a partir de su identificador.
     *
     * @param id identificador único del rol
     * @return información del rol encontrado
     */
    @Override
    public RoleResponse getById(Long id) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.ROLE_NOT_FOUND,
                                "Role with id '%d' was not found."
                                        .formatted(id)));

        return roleMapper.toResponse(role);

    }

}
