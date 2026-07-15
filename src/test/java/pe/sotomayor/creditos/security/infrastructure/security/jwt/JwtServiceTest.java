package pe.sotomayor.creditos.security.infrastructure.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.sotomayor.creditos.security.infrastructure.security.config.JwtProperties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link JwtService}.
 *
 * <p>
 * Verifica la generación, lectura y validación de tokens JWT
 * utilizados por la infraestructura de seguridad.
 * </p>
 *
 * @author DarwinG
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {

        JwtProperties properties = new JwtProperties();

        properties.setSecret(
                "1234567890123456789012345678901234567890123456789012345678901234");

        properties.setExpiration(3_600_000L);

        jwtService = new JwtService(properties);

    }

    @Test
    @DisplayName("Debe generar un token JWT válido")
    void shouldGenerateToken() {

        /* Act */

        String token =
                jwtService.generateToken(
                        "darwin@creditos.com");

        /* Assert */

        assertNotNull(token);

        assertFalse(token.isBlank());

    }

    @Test
    @DisplayName("Debe extraer correctamente el correo electrónico del token")
    void shouldExtractUsername() {

        /* Arrange */

        String token =
                jwtService.generateToken(
                        "darwin@creditos.com");

        /* Act */

        String username =
                jwtService.extractUsername(token);

        /* Assert */

        assertEquals(
                "darwin@creditos.com",
                username
        );

    }

    @Test
    @DisplayName("Debe validar correctamente un token generado")
    void shouldValidateToken() {

        /* Arrange */

        String token =
                jwtService.generateToken(
                        "darwin@creditos.com");

        /* Act */

        boolean valid =
                jwtService.isTokenValid(token);

        /* Assert */

        assertTrue(valid);

    }

    @Test
    @DisplayName("Debe rechazar un token inválido")
    void shouldRejectInvalidToken() {

        /* Act */

        boolean valid =
                jwtService.isTokenValid(
                        "token.invalido");

        /* Assert */

        assertFalse(valid);

    }

    @Test
    @DisplayName("Debe generar tokens diferentes en cada autenticación")
    void shouldGenerateDifferentTokens() {

        /* Act */

        String token1 =
                jwtService.generateToken(
                        "darwin@creditos.com");

        String token2 =
                jwtService.generateToken(
                        "darwin@creditos.com");

        /* Assert */

        assertNotEquals(
                token1,
                token2
        );

    }

}
