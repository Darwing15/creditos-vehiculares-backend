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
 * <p>Reemplaza los tres parseos por una verificacion y separa los claims
 * verificados de la comprobacion posterior de estado y version en BD.
 * El parser y la clave se construyen una vez; la politica se fija al iniciar.</p>
 *
 * @author DarwinG
 */
@Service
public class JwtService {

    /**
     * Politica de emisor, audiencia, duracion y tolerancia; no modificar en caliente.
     */
    private final JwtProperties properties;

    /**
     * Reloj inyectable compartido con las pruebas y los servicios de la aplicacion.
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
     * @param properties configuracion validada por Spring al iniciar
     * @param clock      reloj utilizado para emision y validacion temporal
     * @throws IllegalStateException                si la clave decodificada no tiene 32 bytes
     * @throws io.jsonwebtoken.io.DecodingException si el secreto no es Base64 valido
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
     * Firma un token desde el mismo principal que supero el inicio de sesion.
     *
     * <p>No vuelve a consultar por correo ni incluye contraseña o permisos en el JWT.</p>
     *
     * @param principal identidad autenticada con ID y version inicializados
     * @return JWT compacto firmado con HS256
     * @throws IllegalArgumentException si el principal esta deshabilitado o sin version
     */
    public String generateToken(SecurityPrincipal principal) {
        return generateToken(principal, clock.instant().getEpochSecond());
    }

    private String generateToken(SecurityPrincipal principal, long sessionStarted) {
        if (!principal.isEnabled() || principal.getTokenVersion() == null) {
            throw new IllegalArgumentException("Usuario no habilitado.");
        }
        /* Instante unico para mantener coherencia dentro de esta operacion. */
        Instant now = clock.instant();
        Instant sessionEnd = Instant.ofEpochSecond(sessionStarted).plusMillis(properties.getAbsoluteLifetime());
        Instant tokenEnd = now.plusMillis(properties.getExpiration());
        if (sessionEnd.isBefore(tokenEnd)) tokenEnd = sessionEnd;
        if (!tokenEnd.isAfter(now)) throw new JwtException("Sesión expirada.");
        return Jwts.builder()
                .header().type("at+jwt").and()
                .issuer(properties.getIssuer())
                .audience().add(properties.getAudience()).and()
                .subject(principal.getUserId().toString())
                .id(UUID.randomUUID().toString())
                .claim("tv", principal.getTokenVersion())
                .claim("token_use", "access")
                .claim("session_started", sessionStarted)
                .issuedAt(Date.from(now))
                .expiration(Date.from(tokenEnd))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifica firma, política de claims y consistencia temporal del access token.
     *
     * <p>No comprueba que la cuenta siga activa ni que tv sea la version actual:
     * esas comprobaciones corresponden al filtro despues de cargar la cuenta.</p>
     *
     * @param token JWT compacto extraido de Authorization
     * @return ID y version extraidos de claims verificados
     * @throws JwtException             si la firma, expiracion, tipo o claims son invalidos
     * @throws IllegalArgumentException si el texto de entrada no puede procesarse
     */
    public AccessToken parseAccessToken(String token) {
        // Solo se consulta el payload despues de la verificacion criptografica.
        var signed = parser.parseSignedClaims(token);
        if (!"at+jwt".equals(signed.getHeader().getType())) {
            throw new JwtException("Tipo de token inválido.");
        }
        /* Claims ya verificados; se exige ademas presencia y coherencia de sus campos. */
        Claims claims = signed.getPayload();
        /* issuedAt/expiration delimitan la duracion; subject identifica y version revoca. */
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        String subject = claims.getSubject();
        Long version = claims.get("tv", Long.class);
        /* Instante unico para mantener coherencia dentro de esta operacion. */
        Instant now = clock.instant();
        Long sessionStarted = claims.get("session_started", Long.class);
        if (subject == null || !subject.matches("[1-9][0-9]{0,18}")
                || claims.getId() == null || claims.getId().isBlank()
                || version == null || version < 1
                || issuedAt == null || expiration == null
                || !expiration.after(issuedAt)
                || issuedAt.toInstant().isAfter(
                now.plusSeconds(properties.getClockSkewSeconds()))
                || expiration.toInstant().isAfter(
                issuedAt.toInstant().plusMillis(properties.getExpiration()))
                || (sessionStarted != null && (sessionStarted < 0
                || Instant.ofEpochSecond(sessionStarted).isAfter(issuedAt.toInstant())
                || !now.isBefore(Instant.ofEpochSecond(sessionStarted)
                .plusMillis(properties.getAbsoluteLifetime()))
                || expiration.toInstant().isAfter(Instant.ofEpochSecond(sessionStarted)
                .plusMillis(properties.getAbsoluteLifetime()))))) {
            throw new JwtException("Claims de acceso inválidos.");
        }
        try {
            return new AccessToken(Long.parseLong(subject), version);
        } catch (NumberFormatException ex) {
            throw new JwtException("Identidad inválida.", ex);
        }
    }

    /**
     * Renueva unicamente un JWT vigente y conserva el inicio de la sesion original.
     */
    public RenewedToken renewToken(SecurityPrincipal principal, String token) {
        AccessToken parsed = parseAccessToken(token);
        if (!parsed.userId().equals(principal.getUserId())
                || !parsed.tokenVersion()
                .equals(principal.getTokenVersion()) || !principal.isEnabled()) {
            throw new JwtException("Sesión inválida.");
        }
        Claims claims = parser.parseSignedClaims(token).getPayload();
        Instant now = clock.instant();
        // No permitir renovar un token vencido, incluso durante la tolerancia del parser.
        if (!claims.getExpiration().toInstant().isAfter(now)) throw new JwtException("Sesión expirada.");
        Long originalStart = claims.get("session_started", Long.class);
        long start = originalStart != null ? originalStart : claims.getIssuedAt().toInstant().getEpochSecond();
        Instant end = Instant.ofEpochSecond(start).plusMillis(properties.getAbsoluteLifetime());
        long expiresIn = Math.min(properties.getExpiration() / 1000,
                java.time.Duration.between(now, end).toSeconds());
        if (expiresIn < 1) throw new JwtException("Sesión expirada.");
        return new RenewedToken(generateToken(principal, start), expiresIn);
    }

    public record RenewedToken(String token, long expiresIn) {
    }

    /**
     * Expone la duración nominal en la unidad esperada por el cliente.
     *
     * @return duracion configurada en segundos, truncando fracciones de segundo
     */
    public long expiresInSeconds() {
        return Math.min(properties.getExpiration(), properties.getAbsoluteLifetime()) / 1000;
    }

    /**
     * Transporta unicamente las referencias necesarias despues de validar el JWT.
     *
     * @param userId       ID estable extraido de sub
     * @param tokenVersion version declarada por el JWT, aun por comparar con BD
     */
    public record AccessToken(Long userId, Long tokenVersion) {
    }

}
