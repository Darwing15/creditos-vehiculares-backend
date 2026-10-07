package pe.sotomayor.creditos.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configura las dependencias temporales compartidas
 * por los módulos de la aplicación.
 *
 * <p>
 * Centraliza la fuente de tiempo utilizada por los casos
 * de uso que necesitan registrar instantes de negocio,
 * permitiendo sustituirla por relojes determinísticos
 * durante las pruebas.
 * </p>
 *
 * @author DarwinG
 */
@Configuration
public class TimeConfig {

    /**
     * Proporciona el reloj utilizado por la aplicación.
     *
     * @return reloj del sistema basado en UTC.
     */
    @Bean
    public Clock clock() {

        return Clock.systemUTC();
    }

}
