package pe.sotomayor.creditos.security.application.service.user;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.application.mapper.user.UserMapper;

import pe.sotomayor.creditos.security.application.usecase.user.CreateUserUseCase;

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

/**
 * Implementa el caso de uso para registrar nuevos usuarios.
 *
 * <p>
 * Coordina las validaciones de negocio necesarias para crear un usuario,
 * incluyendo la validación de correo, existencia de rol y sucursal,
 * asignación de relaciones del usuario y cifrado de contraseña.
 * </p>
 *
 * @author DarwinG
 */
@RequiredArgsConstructor
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final BranchRepository branchRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    /**
     * Revalida al actor y protege las invariantes de administración durante la misma
     * transacción en la que este servicio modifica usuarios, roles o sucursales.
     */
    private final SecurityWriteGate securityWriteGate;

    /**
     * Registra un usuario con un rol permitido, una sucursal activa y contraseña codificada.
     *
     * @param request datos del nuevo usuario y relaciones que se solicitan asignar
     * @return información pública del usuario persistido, sin contraseña ni JWT
     * @throws ConflictException         si el correo ya está registrado
     * @throws ResourceNotFoundException si no existe un rol o una sucursal administrable
     * @throws ValidationException       si la sucursal está inactiva
     * @throws AccessDeniedException     si faltan permisos
     *                                   o el rol solicitado contiene privilegios que el actor no posee
     */
    @Override
    @PreAuthorize("hasAuthority('security:users:write') and hasAuthority('security:users:assign-role')")
    public UserResponse create(CreateUserRequest request) {

        /* actor contiene permisos vigentes; crear usuarios incluye asignarles un rol. */
        var actor = securityWriteGate.begin(
                SecurityPermission.USERS_WRITE,
                SecurityPermission.USERS_ASSIGN_ROLE);

        if (userRepository.existsByEmail(request.email())) {
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

        /* El rol destino no puede conceder capacidades superiores a las del creador. */
        actor.requireSubset(role.getPermissions());
        /* Existir en la base de datos no equivale a estar disponible para operar. */
        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new ValidationException(
                    ErrorCode.BRANCH_INACTIVE, "La sucursal está inactiva.");
        }

        User user = userMapper.toEntity(request);

        user.setRole(role);

        user.setBranch(branch);

        user.setPassword(
                passwordEncoder.encode(request.password())
        );

        user.setActive(true);
        /* La versión inicial debe viajar en el INSERT; el DEFAULT SQL no corrige un NULL. */
        user.setTokenVersion(1L);
        /* savedUser es la entidad persistida; flush anticipa restricciones y conflictos. */
        User savedUser = userRepository.saveAndFlush(user);
        securityWriteGate.finish();

        return userMapper.toResponse(savedUser);
    }

}