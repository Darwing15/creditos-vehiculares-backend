package pe.sotomayor.creditos.security.infrastructure.security.jwt;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuración para la autenticación basada en JWT.
 *
 * <p>
 * Centraliza los parámetros necesarios para la generación y validación
 * de tokens JWT, permitiendo externalizar su configuración mediante
 * el archivo {@code application.yml}.
 * </p>
 *
 * <p>
 * Todas las clases de infraestructura relacionadas con JWT deben obtener
 * estos valores a través de esta clase, evitando el uso de constantes
 * embebidas en el código.
 * </p>
 *
 * <pre>
 * security:
 *   jwt:
 *     secret: ...
 *     expiration: 86400000
 * </pre>
 *
 * @author DarwinG
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.security.jwt")
public class JwtProperties {

    /**
     * Clave secreta utilizada para firmar y validar los tokens JWT.
     */
    private String secret;

    /**
     * Tiempo de expiración del token expresado en milisegundos.
     */
    private Long expiration;

}
