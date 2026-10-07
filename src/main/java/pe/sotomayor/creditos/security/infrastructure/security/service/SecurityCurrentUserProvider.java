package pe.sotomayor.creditos.security.infrastructure.security.service;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;

import org.springframework.stereotype.Component;

import pe.sotomayor.creditos.shared.security.CurrentUserProvider;

/**
 * Adapta el principal al puerto de identidad usado por los módulos de negocio.
 *
 * <p>Reemplaza la consulta por correo con el ID estable ya autenticado.
 * Los consumidores conservan la interfaz CurrentUserProvider.</p>
 *
 * @author DarwinG
 */
@Component
@RequiredArgsConstructor
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    /**
     * Origen tipado del principal; no admite que el cliente escoja el actor.
     */
    private final CurrentSecurityPrincipal current;

    /**
     * Devuelve el ID estable del actor de la operación.
     *
     * @return identificador persistente de la cuenta autenticada
     * @throws BadCredentialsException si la ejecución no tiene el principal esperado
     */
    @Override
    public Long getCurrentUserId() {
        return current.get().getUserId();
    }

}
