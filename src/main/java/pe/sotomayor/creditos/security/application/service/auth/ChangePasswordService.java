package pe.sotomayor.creditos.security.application.service.auth;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.OptimisticLockingFailureException;

import org.springframework.security.authentication.BadCredentialsException;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.auth.request.ChangePasswordRequest;

import pe.sotomayor.creditos.security.application.usecase.auth.ChangePasswordUseCase;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.service.CurrentSecurityPrincipal;
import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.PasswordReuseException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Cambia la contraseña de la cuenta autenticada y revoca sus tokens previos.
 *
 * <p>Reemplaza la búsqueda por correo con identidad estable, verifica tv y realiza
 * hash y revocación en la misma transacción. Requiere rowVersion.</p>
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChangePasswordService implements ChangePasswordUseCase {

    /**
     * Repositorio que recupera la cuenta gestionada y persiste hash/versión juntos.
     */
    private final UserRepository users;

    /**
     * Compara el hash actual y genera el nuevo; nunca se persiste contraseña en claro.
     */
    private final PasswordEncoder encoder;

    /**
     * Origen de identidad y versión con las que ingresó la petición.
     */
    private final CurrentSecurityPrincipal current;

    /**
     * Comprueba contraseña actual, evita reutilización y revoca los tokens emitidos.
     *
     * <p>El request debe superar @Valid y la política de longitud del DTO antes de
     * invocar este caso de uso desde MVC. Otros adaptadores deben cumplir esa precondición.</p>
     *
     * @param request contraseña actual y nueva, sin normalización ni recortes
     * @throws BadCredentialsException           si la sesión o contraseña actual no son válidas
     * @throws ValidationException               si la nueva contraseña supera 72 bytes UTF-8
     * @throws PasswordReuseException            si la nueva contraseña coincide con la actual
     * @throws OptimisticLockingFailureException si una escritura concurrente impide guardar la versión leída
     */
    @Override
    public void changePassword(ChangePasswordRequest request) {
        /* El actor procede del contexto, no del correo ni de un ID suministrado. */
        var p = current.get();
        /* La entidad gestionada permite detectar cambios concurrentes con @Version. */
        var user = users.findForAuthenticationById(p.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Sesión inválida."));
        if (!Objects.equals(user.getTokenVersion(), p.getTokenVersion())
                || !encoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("No se pudo validar la contraseña actual.");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR,
                    "La contraseña excede los 72 bytes permitidos por BCrypt.");
        }
        if (encoder.matches(request.newPassword(), user.getPassword())) {
            throw new PasswordReuseException();
        }
        user.setPassword(encoder.encode(request.newPassword()));
        user.increaseTokenVersion();
        users.saveAndFlush(user);
    }
}
