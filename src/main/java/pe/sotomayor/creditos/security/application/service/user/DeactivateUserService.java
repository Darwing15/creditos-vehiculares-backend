package pe.sotomayor.creditos.security.application.service.user;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.application.mapper.user.UserMapper;

import pe.sotomayor.creditos.security.application.usecase.user.DeactivateUserUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;
import pe.sotomayor.creditos.security.domain.repository.BranchRepository;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import java.util.Objects;

/**
 * Implementa el caso de uso para desactivar usuarios existentes.
 *
 * <p>
 * Realiza una desactivación funcional del usuario,
 * manteniendo la información histórica del registro.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
@RequiredArgsConstructor
public class DeactivateUserService implements DeactivateUserUseCase {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final BranchRepository branchRepository;

    private final SecurityWriteGate securityWriteGate;

    /**
     * Desactiva un usuario ajeno al actor y revoca sus sesiones anteriores.
     *
     * @param id identificador del usuario que se desea desactivar
     * @return información pública del usuario en estado inactivo
     * @throws ResourceNotFoundException si el usuario no está disponible
     * @throws ConflictException         si ya está inactivo, es el propio actor o la operación
     *                                   dejaría el sistema sin un administrador operativo
     * @throws AccessDeniedException     si faltan permisos
     *                                   o el usuario posee capacidades superiores a las del actor
     */
    @Override
    @PreAuthorize("hasAuthority('security:users:write')")
    public UserResponse deactivate(Long id) {

        var actor = securityWriteGate.begin(SecurityPermission.USERS_WRITE);

        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.USER_NOT_FOUND,
                                "User with id '%s' not found."
                                        .formatted(id)));

        actor.requireSubset(user.getRole().getPermissions());
        /* actor.userId procede de la sesión; user.id identifica el recurso solicitado. */
        if (Objects.equals(actor.userId(), user.getId())) {
            throw new ConflictException(ErrorCode.SECURITY_SELF_DEACTIVATION,
                    "No puede desactivar su propio usuario.");
        }

        if (!user.getActive()) {
            throw new ConflictException(
                    ErrorCode.USER_INACTIVE,
                    "User with id '%s is already inactive."
                            .formatted(id));
        }

        if (branchRepository.existsByManagerId(id)) {
            throw new ConflictException(
                    ErrorCode.VALIDATION_ERROR, "Retire o cambie al responsable de su sucursal antes de trasladarlo.");
        }

        user.setActive(false);
        /* Invalidar tokens incluso si más adelante se implementa la reactivación. */
        user.increaseTokenVersion();

        User savedUser = userRepository.saveAndFlush(user);
        securityWriteGate.finish();

        return userMapper.toResponse(savedUser);
    }

}
