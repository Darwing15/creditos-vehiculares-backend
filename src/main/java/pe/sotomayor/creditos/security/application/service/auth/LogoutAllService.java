package pe.sotomayor.creditos.security.application.service.auth;

import java.util.Objects;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.OptimisticLockingFailureException;

import org.springframework.security.authentication.BadCredentialsException;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.usecase.auth.LogoutAllUseCase;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.service.CurrentSecurityPrincipal;

/**
 * Revoca todas las sesiones de la cuenta mediante un incremento transaccional de tv.
 *
 * <p>Requiere el campo rowVersion de User para detectar
 * actualizaciones concurrentes y evitar restaurar una versión previa.</p>
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LogoutAllService implements LogoutAllUseCase {

    /**
     * Repositorio de la cuenta cuya versión se incrementará de manera persistente.
     */
    private final UserRepository users;

    /**
     * Principal que fija actor y versión con los que se autorizó la solicitud.
     */
    private final CurrentSecurityPrincipal current;

    /**
     * Incrementa tokenVersion si la sesión sigue correspondiendo a la versión almacenada.
     *
     * <p>saveAndFlush anticipa errores de persistencia; el éxito queda sujeto al commit.</p>
     *
     * @throws BadCredentialsException           si la cuenta no existe o la versión dejó de coincidir
     * @throws OptimisticLockingFailureException si otra transacción modificó la versión JPA antes de confirmar la escritura
     */
    @Override
    public void logoutAll() {
        /* Versión de la petición autorizada: se contrasta con la entidad actual. */
        var p = current.get();
        /* Entidad gestionada que participa en el control optimista de concurrencia. */
        var user = users.findForAuthenticationById(p.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Sesión inválida."));
        if (!Objects.equals(user.getTokenVersion(), p.getTokenVersion())) {
            throw new BadCredentialsException("Sesión inválida.");
        }
        user.increaseTokenVersion();
        users.saveAndFlush(user);
    }
}
