package pe.sotomayor.creditos.security.application.service.role;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.role.request.CreateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

import pe.sotomayor.creditos.security.application.mapper.role.RoleMapper;
import pe.sotomayor.creditos.security.application.usecase.role.CreateRoleUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;

import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;

/**
 * Implementa el caso de uso para registrar nuevos roles en el sistema.
 *
 * <p>
 * Este servicio coordina la creación de un rol, delegando la persistencia al
 * repositorio y la conversión de objetos al mapper correspondiente.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
@RequiredArgsConstructor
public class CreateRoleService implements CreateRoleUseCase {

    private final RoleRepository roleRepository;

    private final RoleMapper roleMapper;

    private final SecurityWriteGate securityWriteGate;

    /**
     * Registra un nuevo rol en el sistema.
     *
     * @param request información del rol a registrar
     * @return información del rol registrado
     */
    @Override
    @PreAuthorize("hasAuthority('security:roles:write')")
    public RoleResponse create(CreateRoleRequest request) {

        securityWriteGate.begin(SecurityPermission.ROLES_WRITE);

        if (roleRepository.existsByName(request.name())) {
            throw new ConflictException(
                    ErrorCode.ROLE_ALREADY_EXISTS,
                    "Role with name '%s' already exists.".formatted(request.name()));
        }

        Role roleToSave = roleMapper.toEntity(request);

        Role savedRole = roleRepository.saveAndFlush(roleToSave);
        securityWriteGate.finish();

        return roleMapper.toResponse(savedRole);
    }
}
