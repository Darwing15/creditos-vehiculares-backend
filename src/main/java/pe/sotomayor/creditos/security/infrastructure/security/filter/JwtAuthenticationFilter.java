package pe.sotomayor.creditos.security.infrastructure.security.filter;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;

import org.springframework.http.HttpHeaders;

import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.web.filter.OncePerRequestFilter;

import pe.sotomayor.creditos.security.infrastructure.security.config.SecurityErrorHandler;
import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

/**
 * Autentica una peticion Bearer mediante firma JWT y estado vigente de la cuenta.
 *
 * <p>Reemplaza la aceptacion basada únicamente en tokenVersion: comprueba tambien
 * los indicadores de UserDetails antes de construir una autenticacion confiable.
 * El resto de la cadena queda fuera del bloque que clasifica errores JWT.</p>
 *
 * @author DarwinG
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Verifica criptografía y claims; no decide disponibilidad de la cuenta.
     */
    private final JwtService jwtService;

    /**
     * Carga desde BD el principal identificado por sub.
     */
    private final CustomUserDetailsService users;

    /**
     * Escribe las respuestas de rechazo antes de llegar a los controladores.
     */
    private final SecurityErrorHandler errors;

    /**
     * Valida los indicadores de UserDetails, incluido enabled, antes de autenticar.
     */
    private final AccountStatusUserDetailsChecker checker =
            new AccountStatusUserDetailsChecker();

    /**
     * Omite JWT en login y solicitudes OPTIONS.
     *
     * <p>Login debe poder procesar credenciales aunque el cliente tenga un JWT vencido.
     * CORS se configura por separado; omitir el filtro no concede por sí solo permisos.</p>
     *
     * @param request solicitud recibida
     * @return true para los caminos que no deben procesar Bearer en este filtro
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return ("POST".equals(request.getMethod())
                && "/api/auth/login".equals(request.getServletPath()))
                || "OPTIONS".equals(request.getMethod());
    }

    /**
     * Valida una única credencial, verifica la cuenta y registra el contexto de seguridad.
     *
     * <p>Sin Authorization deja decidir a la cadena. Ante un JWT inválido responde 401;
     * ante DataAccessException responde 503. No consume errores de negocio posteriores.</p>
     *
     * @param request  solicitud con la cabecera Authorization opcional
     * @param response respuesta HTTP para rechazos del filtro
     * @param chain    continuación del procesamiento autorizado o anonimo
     * @throws ServletException si la cadena posterior produce un error de servlet
     * @throws IOException      si falla la lectura/escritura o la cadena posterior
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        /* Conservar todas las cabeceras para rechazar credenciales duplicadas. */
        var headers = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
        if (headers.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        try {
            /* Límite defensivo del texto antes del parseo criptográfico. */
            String header = headers.getFirst();
            if (headers.size() != 1 || header.length() > 8192
                    || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                throw new BadCredentialsException("Credencial inválida.");
            }
            /* Credencial compacta, sin el esquema Bearer ni espacios adicionales. */
            String token = header.substring(7);
            if (token.isBlank() || token.chars().anyMatch(Character::isWhitespace)) {
                throw new BadCredentialsException("Credencial inválida.");
            }
            /* claims es la proyección verificada; principal incorpora el estado de BD. */
            var claims = jwtService.parseAccessToken(token);
            var principal = users.loadUserById(claims.userId());
            checker.check(principal);
            if (!Objects.equals(claims.tokenVersion(), principal.getTokenVersion())) {
                throw new BadCredentialsException("Sesión revocada.");
            }
            principal.eraseCredentials();
            /* Se marca autenticada únicamente después de comprobar estado y versión. */
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    principal, null, principal.getAuthorities());
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
            /* Aislar la autenticación en un contexto nuevo para esta solicitud. */
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | AuthenticationException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            errors.commence(request, response,
                    new BadCredentialsException("Credencial inválida."));
            return;
        } catch (DataAccessException ex) {
            SecurityContextHolder.clearContext();
            log.error("No se pudo consultar el estado de autenticación.", ex);
            errors.unavailable(request, response);
            return;
        }
        /* Fuera del try: errores de negocio no son errores JWT. */
        chain.doFilter(request, response);
    }

}
