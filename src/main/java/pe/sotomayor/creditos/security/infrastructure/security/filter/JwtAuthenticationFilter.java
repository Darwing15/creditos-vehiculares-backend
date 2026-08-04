package pe.sotomayor.creditos.security.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.web.filter.OncePerRequestFilter;

import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

import java.io.IOException;

/**
 * Filtro encargado de autenticar las solicitudes mediante JWT.
 *
 * <p>
 * Intercepta cada petición HTTP para verificar la existencia de un
 * token JWT válido en el encabezado Authorization. Cuando el token
 * es válido y corresponde a la versión vigente del usuario, registra
 * la autenticación dentro del contexto de Spring Security.
 * </p>
 *
 * <p>
 * Este filtro se ejecuta una única vez por solicitud gracias a la
 * herencia de {@link OncePerRequestFilter}.
 * </p>
 *
 * @author DarwinG
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;

    /**
     * Procesa la autenticación basada en JWT para cada solicitud.
     *
     * @param request     petición HTTP
     * @param response    respuesta HTTP
     * @param filterChain cadena de filtros
     * @throws ServletException si ocurre un error del servlet
     * @throws IOException      si ocurre un error de entrada o salida
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = extractJwt(request);

        if (jwt != null) {
            authenticateRequest(jwt, request);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token JWT del encabezado Authorization.
     *
     * @param request petición HTTP
     * @return JWT o {@code null} cuando no existe
     */
    private String extractJwt(HttpServletRequest request) {

        String authorization =
                request.getHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            return null;
        }

        return authorization.substring(7);
    }

    /**
     * Autentica la solicitud utilizando el token JWT recibido.
     *
     * <p>
     * Si ocurre cualquier error durante el proceso de autenticación,
     * el contexto de seguridad es limpiado y la solicitud continúa
     * sin autenticación.
     * </p>
     *
     * @param jwt     token JWT
     * @param request petición HTTP
     */
    private void authenticateRequest(
            String jwt,
            HttpServletRequest request) {

        try {

            String email = jwtService.extractUsername(jwt);

            if (email == null
                    || hasAuthentication()
                    || !jwtService.isTokenValid(jwt)) {
                return;
            }

            Long tokenVersion =
                    jwtService.extractTokenVersion(jwt);

            User user =
                    userDetailsService.loadDomainUser(email);

            if (!tokenVersion.equals(user.getTokenVersion())) {

                return;
            }

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

            SecurityContextHolder.getContext()
                    .setAuthentication(
                            createAuthentication(
                                    userDetails,
                                    request));

        } catch (Exception ex) {

            log.error("Error authenticating JWT.", ex);

            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Indica si la solicitud ya posee una autenticación registrada.
     *
     * @return {@code true} cuando existe una autenticación previa
     */
    private boolean hasAuthentication() {

        return SecurityContextHolder
                .getContext()
                .getAuthentication() != null;
    }

    /**
     * Construye la autenticación utilizada por Spring Security.
     *
     * @param userDetails información del usuario autenticado
     * @param request     petición HTTP
     * @return autenticación completamente inicializada
     */
    private UsernamePasswordAuthenticationToken createAuthentication(
            UserDetails userDetails,
            HttpServletRequest request) {

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

        authentication.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request));

        return authentication;
    }

}
