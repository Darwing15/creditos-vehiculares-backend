package pe.sotomayor.creditos.security.infrastructure.security.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;

/**
 * Centraliza la obtención tipada de la identidad en el contexto de Spring Security.
 *
 * <p>Evita repetir comprobaciones de contexto en cada servicio. No consulta BD
 * ni verifica nuevamente el JWT: presupone la cadena de autenticación configurada.</p>
 *
 * @author DarwinG
 */
@Component
public class CurrentSecurityPrincipal {

    /**
     * Obtiene el principal autenticado de la ejecución actual.
     *
     * @return principal del tipo esperado por esta aplicación
     * @throws BadCredentialsException si no hay autenticación válida del tipo esperado
     */
    public SecurityPrincipal get() {
        /* Contexto de esta ejecución; no procede de un ID recibido en el cuerpo HTTP. */
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof SecurityPrincipal principal)) {
            throw new BadCredentialsException("Autenticación requerida.");
        }
        return principal;
    }
}
