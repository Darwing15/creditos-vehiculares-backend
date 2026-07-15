package pe.sotomayor.creditos.security.infrastructure.security.config;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import pe.sotomayor.creditos.security.infrastructure.security.filter.JwtAuthenticationFilter;
import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

/**
 * Configuración principal de Spring Security.
 *
 * <p>
 * Define la política de autenticación y autorización de la aplicación,
 * registrando el filtro JWT, la gestión sin estado de las sesiones y
 * los componentes necesarios para el proceso de autenticación.
 * </p>
 *
 * @author DarwinG
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;

    /**
     * Registra el filtro JWT dentro del contenedor de Spring.
     *
     * @return filtro JWT
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {

        return new JwtAuthenticationFilter(
                jwtService,
                userDetailsService
        );

    }

    /**
     * Configura la cadena de filtros de seguridad.
     *
     * @param http                    configuración HTTP
     * @param jwtAuthenticationFilter filtro JWT
     * @return cadena de filtros configurada
     * @throws Exception si ocurre un error durante la configuración
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Expone el AuthenticationManager administrado por Spring Security.
     *
     * @param configuration configuración de autenticación
     * @return administrador de autenticación
     * @throws Exception si ocurre un error al obtener el componente
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();

    }

    /**
     * Codificador de contraseñas utilizado por la aplicación.
     *
     * @return codificador BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();

    }

}
