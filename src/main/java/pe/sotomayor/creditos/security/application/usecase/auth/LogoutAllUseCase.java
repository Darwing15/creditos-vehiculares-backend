package pe.sotomayor.creditos.security.application.usecase.auth;

/**
 * Define la revocación global de los access tokens de la cuenta autenticada.
 *
 * <p>Se agrega como puerto de aplicación para que el controlador no gestione
 * persistencia ni versiones. El cierre individual requiere otro modelo de sesión.</p>
 *
 * @author DarwinG
 */
public interface LogoutAllUseCase {

    /**
     * Revoca tokens emitidos con versiones anteriores de la cuenta actual.
     *
     * <p>La revocación efectiva depende de que la transacción se confirme.</p>
     */
    void logoutAll();
}
