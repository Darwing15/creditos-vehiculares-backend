package pe.sotomayor.creditos.security.application.service.auth;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AnonymousAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.auth.request.ChangePasswordRequest;

import pe.sotomayor.creditos.security.application.usecase.auth.ChangePasswordUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.PasswordReuseException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.UnauthorizedException;

/**
 * Implementa el caso de uso encargado de actualizar la contraseña
 * del usuario autenticado.
 *
 * <p>
 * Recupera el usuario autenticado desde el contexto de seguridad,
 * valida la contraseña actual y registra una nueva contraseña
 * cifrada. Como parte del proceso incrementa la versión del token,
 * invalidando automáticamente todos los JWT emitidos previamente.
 * </p>
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChangePasswordService implements ChangePasswordUseCase {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * Actualiza la contraseña del usuario autenticado.
     *
     * @param request datos necesarios para realizar el cambio de contraseña
     */
    @Override
    public void changePassword(ChangePasswordRequest request) {

        Authentication authentication = getAuthentication();

        User user = findAuthenticatedUser(authentication.getName());

        validateCurrentPassword(user, request);

        validateNewPassword(user, request);

        updatePassword(user, request);

        userRepository.save(user);
    }

    /**
     * Obtiene la autenticación actual desde el contexto de seguridad.
     *
     * @return autenticación del usuario
     * @throws UnauthorizedException si no existe un usuario autenticado
     */
    private Authentication getAuthentication() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new UnauthorizedException(
                    ErrorCode.UNAUTHORIZED,
                    "User is not authenticated.");
        }

        return authentication;
    }

    /**
     * Recupera el usuario autenticado desde la base de datos.
     *
     * @param email correo electrónico autenticado
     * @return usuario encontrado
     * @throws ResourceNotFoundException si el usuario no existe
     */
    private User findAuthenticatedUser(String email) {

        User user = userRepository
                .findUserWithRoleAndBranchByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.USER_NOT_FOUND,
                                "User not found."));

        if (!Boolean.TRUE.equals(user.getActive())) {

            throw new UnauthorizedException(
                    ErrorCode.UNAUTHORIZED,
                    "User is inactive.");
        }

        return user;
    }

    /**
     * Valida que la contraseña actual proporcionada sea correcta.
     *
     * @param user    usuario autenticado
     * @param request datos del cambio de contraseña
     * @throws UnauthorizedException si la contraseña actual no coincide
     */
    private void validateCurrentPassword(
            User user,
            ChangePasswordRequest request) {

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPassword())) {

            throw new UnauthorizedException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Current password is incorrect.");
        }
    }

    /**
     * Valida que la nueva contraseña sea diferente de la actual.
     *
     * @param user    usuario autenticado
     * @param request datos del cambio de contraseña
     * @throws PasswordReuseException si ambas contraseñas coinciden
     */
    private void validateNewPassword(
            User user,
            ChangePasswordRequest request) {

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPassword())) {

            throw new PasswordReuseException();
        }
    }

    /**
     * Actualiza la contraseña del usuario e incrementa la versión
     * del token para invalidar los JWT emitidos anteriormente.
     *
     * @param user    usuario autenticado
     * @param request datos del cambio de contraseña
     */
    private void updatePassword(
            User user,
            ChangePasswordRequest request) {

        user.setPassword(
                passwordEncoder.encode(request.newPassword()));

        user.increaseTokenVersion();
    }
}
