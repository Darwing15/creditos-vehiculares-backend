package pe.sotomayor.creditos.security.application.service.user;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.user.request.UpdateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.application.mapper.user.UserMapper;
import pe.sotomayor.creditos.security.application.usecase.user.UpdateUserUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;
import pe.sotomayor.creditos.security.domain.repository.BranchRepository;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.util.Objects;

/**
 * Implementa el caso de uso para actualizar usuarios existentes.
 *
 * <p>
 * Gestiona la actualización de información administrativa del usuario,
 * incluyendo validación de correo, existencia de rol y sucursal.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
@RequiredArgsConstructor
public class UpdateUserService implements UpdateUserUseCase {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final BranchRepository branchRepository;

    private final UserMapper userMapper;

    private final SecurityWriteGate securityWriteGate;

    /**
     * Actualiza un usuario y revoca sus sesiones cuando cambian rol, sucursal o correo.
     *
     * @param id      identificador del usuario que se modifica, distinto de la identidad del actor
     * @param request metadatos y relaciones destino solicitados
     * @return información pública del usuario actualizado
     * @throws ResourceNotFoundException si el usuario, el rol o la sucursal no están disponibles
     * @throws ConflictException         si el correo está ocupado o se pierde el último administrador
     * @throws ValidationException       si la sucursal está inactiva
     * @throws AccessDeniedException     si el actor no puede
     *                                   administrar al usuario o conceder el rol solicitado
     */
    @Override
    @PreAuthorize("hasAuthority('security:users:write')")
    public UserResponse update(Long id, UpdateUserRequest request) {

        var actor = securityWriteGate.begin(SecurityPermission.USERS_WRITE);

        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.USER_NOT_FOUND,
                                "User with id '%s' not found."
                                        .formatted(id)));

        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {

            throw new ConflictException(
                    ErrorCode.USER_ALREADY_EXISTS,
                    "User with email '%s' already exists."
                            .formatted(request.email()));
        }

        Role role = roleRepository.findByIdAndDeletedFalse(request.roleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.ROLE_NOT_FOUND,
                                "Role with id '%s' not found."
                                        .formatted(request.roleId())));

        Branch branch = branchRepository.findByIdAndDeletedFalse(request.branchId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.BRANCH_NOT_FOUND,
                                "Branch with id '%s' not found."
                                        .formatted(request.branchId())));

        /* Comprobar el rol actual evita editar usuarios con privilegios superiores. */
        actor.requireSubset(user.getRole().getPermissions());

        /* Capturar los cambios antes del mapper: estos tres valores determinan revocación.
         roleChanged exige además la autorización específica para asignar roles. */
        boolean roleChanged = !Objects.equals(user.getRole().getId(), role.getId());
        boolean branchChanged = !Objects.equals(user.getBranch().getId(), branch.getId());
        boolean emailChanged = !Objects.equals(user.getEmail(), request.email());

        if (roleChanged) {
            actor.require(SecurityPermission.USERS_ASSIGN_ROLE);
            actor.requireSubset(role.getPermissions());
        }
        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new ValidationException(
                    ErrorCode.BRANCH_INACTIVE, "La sucursal está inactiva.");
        }

        if (branchChanged && branchRepository.existsByManagerId(id)) {
            throw new ConflictException(
                    ErrorCode.VALIDATION_ERROR, "Retire o cambie al responsable de su sucursal antes de trasladarlo.");
        }

        userMapper.updateEntity(request, user);

        user.setRole(role);

        user.setBranch(branch);

        /* Una sesión anterior no debe seguir operando con otra identidad o alcance. */
        if (roleChanged || branchChanged || emailChanged) {
            user.increaseTokenVersion();
        }
        User updatedUser = userRepository.saveAndFlush(user);
        securityWriteGate.finish();

        return userMapper.toResponse(updatedUser);
    }

}
