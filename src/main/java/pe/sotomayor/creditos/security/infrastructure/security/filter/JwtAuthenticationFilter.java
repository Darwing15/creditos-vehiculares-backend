package pe.sotomayor.creditos.security.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

import java.io.IOException;

/**
 * Filtro encargado de autenticar las solicitudes mediante JWT.
 *
 * <p>
 * Intercepta cada petición HTTP para verificar la existencia de un
 * token JWT válido en el encabezado Authorization. Si el token es
 * correcto, construye el contexto de autenticación utilizado por
 * Spring Security.
 * </p>
 *
 * <p>
 * Este filtro se ejecuta una única vez por solicitud gracias a la
 * herencia de {@link OncePerRequestFilter}.
 * </p>
 *
 * @author DarwinG
 */
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

        final String authorization =
                request.getHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);

            return;
        }

        String jwt = authorization.substring(7);

        try {
            String username = jwtService.extractUsername(jwt);

            if (username != null
                    && SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt)) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request));

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

}
