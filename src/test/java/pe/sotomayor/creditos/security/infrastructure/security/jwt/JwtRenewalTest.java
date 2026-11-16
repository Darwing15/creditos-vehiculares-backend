package pe.sotomayor.creditos.security.infrastructure.security.jwt;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;

import java.nio.charset.StandardCharsets;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class JwtRenewalTest {
    private static final Instant START = Instant.parse("2026-11-05T10:00:00Z");
    private final SecurityPrincipal principal = new SecurityPrincipal(
            1L,
            1L,
            "Usuario",
            "a@b.pe",
            "hash",
            1L,
            "rol",
            1L,
            "sucursal",
            true,
            List.of()
    );

    private JwtService at(long seconds) {
        JwtProperties props = new JwtProperties();
        props.setSecret(Base64.getEncoder()
                .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)));
        props.setIssuer("creditos-api");
        props.setAudience("creditos-web");
        props.setClockSkewSeconds(30);
        return new JwtService(props, Clock.fixed(START.plusSeconds(seconds), ZoneOffset.UTC));
    }

    @Test
    void activeSessionCanRenewWithinTenMinutes() {

        /* Arrange */

        String original = at(0).generateToken(principal);

        /* Act */

        var renewed = at(540).renewToken(principal, original);
        var parsed = at(1100).parseAccessToken(renewed.token());

        /* Assert */

        assertThat(renewed.expiresIn()).isEqualTo(600);
        assertThat(parsed.userId()).isEqualTo(1L);
    }

    @Test
    void expiredTokenCannotBeRenewedEvenWithinClockSkew() {

        /* Arrange */

        String original = at(0).generateToken(principal);

        /* Act */

        Throwable thrown = catchThrowable(() -> at(600)
                .renewToken(principal, original));

        /* Assert */

        assertThat(thrown)
                .isInstanceOf(JwtException.class);
    }

    @Test
    void renewalPreservesAbsoluteDeadline() {

        /* Arrange */

        String token = at(0).generateToken(principal);
        for (int seconds = 540; seconds <= 28080; seconds += 540) {
            token = at(seconds).renewToken(principal, token).token();
        }

        /* Act */

        var renewed = at(28600).renewToken(principal, token);
        Throwable thrown = catchThrowable(() -> at(28800)
                .parseAccessToken(renewed.token()));

        /* Assert */

        assertThat(renewed.expiresIn()).isEqualTo(200);
        assertThat(thrown)
                .isInstanceOf(JwtException.class);
    }

    @Test
    void cannotRenewForADifferentIdentityOrVersion() {

        /* Arrange */

        String token = at(0).generateToken(principal);
        var revoked = new SecurityPrincipal(
                1L,
                2L,
                "Usuario",
                "a@b.pe",
                "hash",
                1L,
                "rol",
                1L,
                "sucursal",
                true,
                List.of()
        );

        /* Act */

        var failure = catchThrowable(() -> at(1)
                .renewToken(revoked, token));

        /* Assert */

        assertThat(failure).isInstanceOf(JwtException.class);
    }
}
