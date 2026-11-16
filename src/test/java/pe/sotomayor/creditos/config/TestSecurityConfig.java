package pe.sotomayor.creditos.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/*
 * Configuracion de seguridad utilizada exclusivamente durante
 * las pruebas de la capa de presentacion.
 *
 * <p>
 * Deshabilita la autenticacion JWT para permitir que los
 * controladores sean probados de manera aislada.
 * </p>
 *
 * @author DarwinG
 */
@TestConfiguration
public class TestSecurityConfig {

    /*
     * Configura una cadena de seguridad que permite todas
     * las solicitudes durante las pruebas.
     *
     * @param http configuración HTTP
     * @return cadena de filtros
     * @throws Exception si ocurre un error
     */
    @Bean
    SecurityFilterChain testSecurityFilterChain(
            HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth ->
                        auth.anyRequest().permitAll());

        return http.build();

    }

}
