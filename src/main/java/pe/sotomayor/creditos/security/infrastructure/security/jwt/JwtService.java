package pe.sotomayor.creditos.security.infrastructure.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;

import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;

import javax.crypto.SecretKey;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Emite y verifica exclusivamente los access tokens de esta API.
 *
 * <p>Reemplaza los tres parseos por una verificación y separa los claims
 * verificados de la comprobación posterior de estado y versión en BD.
 * El parser y la clave se construyen una vez; la política se fija al iniciar.</p>
 *
 * @author DarwinG
 */
@Service
public class JwtService {

    /**
     * Política de emisor, audiencia, duración y tolerancia; no modificar en caliente.
     */
    private final JwtProperties properties;

    /**
     * Reloj inyectable compartido con las pruebas y los servicios de la aplicación.
     */
    private final Clock clock;

    /**
     * Clave HS256 ya decodificada; evita reconstruirla en cada solicitud.
     */
    private final SecretKey key;

    /**
     * Verificador reutilizable con algoritmo, emisor, audiencia y uso restringidos.
     */
    private final JwtParser parser;

    /**
     * Valida la clave y construye el parser con una única política de confianza.
     *
     * @param properties configuración validada por Spring al iniciar
     * @param clock      reloj utilizado para emisión y validación temporal
     * @throws IllegalStateException                si la clave decodificada no tiene 32 bytes
     * @throws io.jsonwebtoken.io.DecodingException si el secreto no es Base64 válido
     */
    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        /* Material secreto decodificado: validar longitud antes de configurar HS256. */
        byte[] bytes = Decoders.BASE64.decode(properties.getSecret());
        if (bytes.length != 32) {
            throw new IllegalStateException(
                    "JWT_SECRET_BASE64 debe codificar exactamente 32 bytes.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.parser = Jwts.parser()
                .verifyWith(key)
                .sig().clear().add(Jwts.SIG.HS256).and()
                .requireIssuer(properties.getIssuer())
                .requireAudience(properties.getAudience())
                .require("token_use", "access")
                .clock(() -> Date.from(clock.instant()))
                .clockSkewSeconds(properties.getClockSkewSeconds())
                .build();
    }

    /**
     * Firma un token desde el mismo principal que superó el inicio de sesión.
     *
     * <p>No vuelve a consultar por correo ni incluye contraseña o permisos en el JWT.</p>
     *
     * @param principal identidad autenticada con ID y versión inicializados
     * @return JWT compacto firmado con HS256
     * @throws IllegalArgumentException si el principal está deshabilitado o sin versión
     */
    public String generateToken(SecurityPrincipal principal) {
        if (!principal.isEnabled() || principal.getTokenVersion() == null) {
            throw new IllegalArgumentException("Usuario no habilitado.");
        }
        /* Instante único para mantener coherencia dentro de esta operación. */
        Instant now = clock.instant();
        return Jwts.builder()
                .header().type("at+jwt").and()
                .issuer(properties.getIssuer())
                .audience().add(properties.getAudience()).and()
                .subject(principal.getUserId().toString())
                .id(UUID.randomUUID().toString())
                .claim("tv", principal.getTokenVersion())
                .claim("token_use", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(properties.getExpiration())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifica firma, política de claims y consistencia temporal del access token.
     *
     * <p>No comprueba que la cuenta siga activa ni que tv sea la versión actual:
     * esas comprobaciones corresponden al filtro después de cargar la cuenta.</p>
     *
     * @param token JWT compacto extraído de Authorization
     * @return ID y versión extraídos de claims verificados
     * @throws JwtException             si la firma, expiración, tipo o claims son inválidos
     * @throws IllegalArgumentException si el texto de entrada no puede procesarse
     */
    public AccessToken parseAccessToken(String token) {
        // Solo se consulta el payload después de la verificación criptográfica.
        var signed = parser.parseSignedClaims(token);
        if (!"at+jwt".equals(signed.getHeader().getType())) {
            throw new JwtException("Tipo de token inválido.");
        }
        /* Claims ya verificados; se exige además presencia y coherencia de sus campos. */
        Claims claims = signed.getPayload();
        /* issuedAt/expiration delimitan la duración; subject identifica y version revoca. */
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        String subject = claims.getSubject();
        Long version = claims.get("tv", Long.class);
        /* Instante único para mantener coherencia dentro de esta operación. */
        Instant now = clock.instant();
        if (subject == null || !subject.matches("[1-9][0-9]{0,18}")
                || claims.getId() == null || claims.getId().isBlank()
                || version == null || version < 1
                || issuedAt == null || expiration == null
                || !expiration.after(issuedAt)
                || issuedAt.toInstant().isAfter(
                now.plusSeconds(properties.getClockSkewSeconds()))
                || expiration.toInstant().isAfter(
                issuedAt.toInstant().plusMillis(properties.getExpiration()))) {
            throw new JwtException("Claims de acceso inválidos.");
        }
        try {
            return new AccessToken(Long.parseLong(subject), version);
        } catch (NumberFormatException ex) {
            throw new JwtException("Identidad inválida.", ex);
        }
    }

    /**
     * Expone la duración nominal en la unidad esperada por el cliente.
     *
     * @return duración configurada en segundos, truncando fracciones de segundo
     */
    public long expiresInSeconds() {
        return properties.getExpiration() / 1000;
    }

    /**
     * Transporta únicamente las referencias necesarias después de validar el JWT.
     *
     * @param userId       ID estable extraído de sub
     * @param tokenVersion versión declarada por el JWT, aún por comparar con BD
     */
    public record AccessToken(Long userId, Long tokenVersion) {
    }

}
