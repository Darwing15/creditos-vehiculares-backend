package pe.sotomayor.creditos.security.application.service.role;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;
import pe.sotomayor.creditos.security.application.mapper.role.RoleMapper;
import pe.sotomayor.creditos.security.application.usecase.role.ListRolesUseCase;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;

import java.util.List;

/**
 * Implementa el caso de uso para listar roles disponibles.
 *
 * <p>
 * Obtiene los roles registrados en el sistema y delega la transformación
 * de entidades hacia DTOs al mapper correspondiente.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListRolesService implements ListRolesUseCase {

    private final RoleRepository roleRepository;

    private final RoleMapper roleMapper;

    /**
     * Obtiene todos los roles disponibles.
     *
     * @return lista de roles
     */
    @Override
    @PreAuthorize("hasAuthority('security:roles:read')")
    public List<RoleResponse> findAll() {

        return roleRepository.findAllByDeletedFalse()
                .stream()
                .map(roleMapper::toResponse)
                .toList();
    }

}
