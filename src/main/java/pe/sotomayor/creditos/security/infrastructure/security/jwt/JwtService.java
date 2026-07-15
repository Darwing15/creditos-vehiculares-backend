package pe.sotomayor.creditos.security.infrastructure.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import pe.sotomayor.creditos.security.infrastructure.security.config.JwtProperties;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Servicio encargado de la generación y validación de tokens JWT.
 *
 * <p>
 * Centraliza todas las operaciones relacionadas con JSON Web Tokens,
 * incluyendo su creación, validación y extracción de información
 * contenida en los claims.
 * </p>
 *
 * <p>
 * Este servicio pertenece a la capa de infraestructura y constituye
 * el único punto de acceso para manipular tokens dentro del sistema.
 * </p>
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Genera un token JWT para el usuario indicado.
     *
     * @param username correo electrónico del usuario autenticado
     * @return token firmado
     */
    public String generateToken(String username) {

        Date now = new Date();

        Date expiration =
                new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Obtiene el nombre de usuario contenido dentro del token.
     *
     * @param token token JWT
     * @return correo electrónico del usuario
     */
    public String extractUsername(String token) {

        return extractClaims(token)
                .getSubject();
    }

    /**
     * Verifica si el token es válido.
     *
     * @param token token JWT
     * @return {@code true} si el token es válido
     */
    public boolean isTokenValid(String token) {

        try {
            extractClaims(token);

            return true;

        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Obtiene los claims contenidos en el token.
     *
     * @param token token JWT
     * @return claims del token
     */
    private Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Construye la clave utilizada para firmar y validar tokens.
     *
     * @return clave HMAC
     */
    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                jwtProperties.getSecret()
                        .getBytes(StandardCharsets.UTF_8));
    }

}
