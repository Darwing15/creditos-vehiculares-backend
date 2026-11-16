package pe.sotomayor.creditos.security.infrastructure.security.jwt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Agrupa y valida al iniciar la política de emisión y lectura de JWT.
 *
 * <p>Se reemplaza la configuración permisiva por límites explícitos y una clave
 * externa. Los accesores generados por Lombok permiten el binding de Spring;
 * estas propiedades no deben modificarse después de construir {@code JwtService}.</p>
 *
 * @author DarwinG
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "spring.security.jwt")
public class JwtProperties {

    /**
     * Clave aleatoria de 32 bytes, codificada en Base64 y suministrada por el entorno.
     * No representa una contraseña ni debe registrarse en logs.
     */
    @NotBlank
    private String secret;

    /**
     * Vida nominal del access token en milisegundos; entre uno y quince minutos.
     * El valor inicial de diez minutos limita la duración de una credencial robada.
     */
    @NotNull
    @Min(60000)
    @Max(900000)
    private Long expiration = 600000L;

    /**
     * Duracion maxima continua de la sesion, independientemente de la actividad.
     */
    @Min(600000)
    @Max(86400000)
    private long absoluteLifetime = 28800000L;

    /**
     * Identificador del emisor que debe coincidir al generar y verificar el token.
     */
    @NotBlank
    private String issuer;

    /**
     * Destinatario esperado; separa estos tokens de otros usos de la misma infraestructura.
     */
    @NotBlank
    private String audience;

    /**
     * Tolerancia en segundos para pequeñas diferencias de reloj.
     * Puede admitir brevemente un token después de su expiración nominal.
     */
    @Min(0)
    @Max(60)
    private long clockSkewSeconds = 30;

}
