package pe.sotomayor.creditos.security.infrastructure.security.config;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import pe.sotomayor.creditos.security.infrastructure.security.filter.JwtAuthenticationFilter;
import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

import java.util.Arrays;
import java.util.List;

/**
 * Configura autenticacion, CORS y restricciones HTTP para el modulo de seguridad.
 *
 * <p>
 * Reemplaza la apertura general de rutas de autenticacion por endpoints concretos.
 * La proteccion de operaciones depende ademas de las reglas de servicios.
 * No habilita cookies de autenticacion ni renovacion automatica.
 * </p>
 *
 * @author DarwinG
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Proporciona BCrypt compatible con los hashes actuales para verificar credenciales.
     *
     * <p>El coste 12 afecta hashes nuevos; debe calibrarse con la capacidad del despliegue.</p>
     *
     * @return codificador BCrypt con coste inicial 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Registra explicitamente el proveedor que comprueba correo y contrasena.
     *
     * @param users   cargador del principal y de su estado de acceso
     * @param encoder verificador compatible con los hashes persistidos
     * @return administrador que delega en el proveedor DAO configurado
     */
    @Bean
    public AuthenticationManager authenticationManager(
            CustomUserDetailsService users, PasswordEncoder encoder) {

        /* Un solo proveedor explicito elimina ambiguedad sobre el cargador utilizado. */
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    /**
     * Define los orígenes y cabeceras admitidos para llamadas de navegador a la API.
     *
     * <p>Las cookies no participan en este contrato; Authorization Bearer se envía
     * explícitamente. maxAge solo cachea preflight, no prolonga los tokens.</p>
     *
     * @param allowedOrigins lista de orígenes explícitos separados por comas
     * @return política CORS restringida a las rutas /api
     * @throws IllegalStateException si no hay orígenes o contienen comodín/origen null
     */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(
            @Value("${app.security.allowed-origins}") String allowedOrigins) {

        /* Normalizar la lista del entorno antes de validar la politica. */
        var origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::strip).filter(s -> !s.isEmpty()).toList();

        if (origins.isEmpty()
                || origins.stream().anyMatch(s -> s.contains("*") || s.equals("null"))) {
            throw new IllegalStateException("Defina orígenes CORS explícitos.");
        }

        /* Reglas para peticiones del navegador; no sustituyen autorizacion de la API. */
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cors.setExposedHeaders(List.of("WWW-Authenticate"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);

        /* Asociar la politica unicamente al espacio de rutas de la API. */
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);

        return source;
    }

    /**
     * Construye la cadena HTTP y registra el filtro JWT una sola vez dentro de ella.
     *
     * <p>Deshabilitar CSRF depende del contrato Bearer sin credenciales automaticas.
     * Cambiar ese contrato exige revisar la configuracion antes de usar cookies.</p>
     *
     * @param http        constructor de la cadena suministrado por Spring
     * @param jwt         servicio de firma y validación JWT
     * @param users       cargador de identidades y permisos
     * @param errors      traductor de rechazos anteriores a Spring MVC
     * @param cors        origenes y cabeceras permitidos para la API
     * @param docsEnabled habilita documentacion publica solo en el entorno elegido
     * @return cadena configurada con denegacion de rutas no habilitadas
     * @throws Exception si Spring no puede construir la configuracion
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtService jwt, CustomUserDetailsService users,
            SecurityErrorHandler errors, UrlBasedCorsConfigurationSource cors,
            @Value("${app.security.docs-enabled:false}") boolean docsEnabled)
            throws Exception {

        /* No registrar como @Bean: evita registro servlet adicional del filtro. */
        var jwtFilter = new JwtAuthenticationFilter(jwt, users, errors);

        http.cors(c -> c.configurationSource(cors))
                .csrf(c -> c.disable())
                .formLogin(c -> c.disable())
                .httpBasic(c -> c.disable())
                .logout(c -> c.disable())
                .requestCache(c -> c.disable())
                .sessionManagement(c ->
                        c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(c -> c
                        .authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated();
                    auth.requestMatchers(HttpMethod.POST,
                                    "/api/auth/change-password", "/api/auth/logout-all", "/api/auth/renew")
                            .authenticated();
                    if (docsEnabled) {
                        auth.requestMatchers("/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    }
                    auth.requestMatchers("/api/security/**").authenticated();
                    auth.requestMatchers(HttpMethod.GET, "/api/customers",
                                    "/api/customers/page", "/api/customers/{code}")
                            .hasAuthority("customers:read");
                    auth.requestMatchers(HttpMethod.POST, "/api/customers",
                                    "/api/customers/identity-lookups")
                            .hasAuthority("customers:write");
                    auth.requestMatchers(HttpMethod.PUT, "/api/customers/{code}")
                            .hasAuthority("customers:write");
                    auth.requestMatchers(HttpMethod.GET, "/api/vehicles",
                                    "/api/vehicles/page", "/api/vehicles/overview",
                                    "/api/vehicles/{code}")
                            .hasAuthority("vehicles:read");
                    auth.requestMatchers(HttpMethod.POST, "/api/vehicles")
                            .hasAuthority("vehicles:write");
                    auth.requestMatchers(HttpMethod.PUT, "/api/vehicles/{code}")
                            .hasAuthority("vehicles:write");
                    auth.requestMatchers(HttpMethod.GET, "/api/credit-applications",
                                    "/api/credit-applications/page",
                                    "/api/credit-applications/{applicationCode}")
                            .hasAuthority("credit-applications:read");
                    auth.requestMatchers(HttpMethod.POST, "/api/credit-applications",
                                    "/api/credit-applications/simulations")
                            .hasAuthority("credit-applications:create");
                    auth.requestMatchers(HttpMethod.PATCH,
                                    "/api/credit-applications/{applicationCode}/approve",
                                    "/api/credit-applications/{applicationCode}/reject",
                                    "/api/credit-applications/{applicationCode}/cancel")
                            .hasAuthority("credit-applications:resolve");
                    auth.anyRequest().denyAll();
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}
