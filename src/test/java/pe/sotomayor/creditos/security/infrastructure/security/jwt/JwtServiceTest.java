package pe.sotomayor.creditos.security.infrastructure.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class JwtServiceTest {

    @Test
    void shouldGenerateExpectedClaimsWhenPrincipalIsEnabled() throws Exception {
        /* Arrange */
        JwtProperties properties = createProperties();
        Clock clock = createClock();
        JwtService service = new JwtService(properties, clock);
        SecurityPrincipal principal = createPrincipal();

        /* Act */
        String token = service.generateToken(principal);
        Jws<Claims> signed = Jwts.parser().verifyWith(createKey()).clock(() -> Date.from(clock.instant())).build().parseSignedClaims(token);
        JwtService.AccessToken parsed = service.parseAccessToken(token);

        /* Assert */
        assertThat(signed.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(signed.getHeader().getType()).isEqualTo("at+jwt");
        assertThat(signed.getPayload().getSubject()).isEqualTo("1");
        assertThat(signed.getPayload().getIssuer()).isEqualTo("creditos-api");
        assertThat(signed.getPayload().getAudience()).containsExactly("creditos-web");
        assertThat(signed.getPayload().getId()).isNotBlank();
        assertThat(signed.getPayload().get("token_use")).isEqualTo("access");
        assertThat(signed.getPayload().getIssuedAt().toInstant()).isEqualTo(clock.instant());
        assertThat(signed.getPayload().getExpiration().toInstant()).isEqualTo(clock.instant().plusSeconds(600));
        assertThat(signed.getPayload()).doesNotContainKeys("password", "email", "permissions", "role");
        assertThat(parsed.userId()).isEqualTo(1L);
        assertThat(parsed.tokenVersion()).isEqualTo(1L);
        assertThat(service.expiresInSeconds()).isEqualTo(600);
    }

    @Test
    void shouldGenerateDistinctIdsWhenIssuingTwoTokens() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        SecurityPrincipal principal = createPrincipal();

        /* Act */
        String first = service.generateToken(principal);
        String second = service.generateToken(principal);

        /* Assert */
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldRejectGenerationWhenPrincipalIsDisabled() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        SecurityPrincipal valid = createPrincipal();
        SecurityPrincipal principal = new SecurityPrincipal(valid.getUserId(), 1L, valid.getFullName(), valid.getUsername(), valid.getPassword(), valid.getRoleId(), valid.getRoleName(), valid.getBranchId(), valid.getBranchName(), false, valid.getAuthorities());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.generateToken(principal));

        /* Assert */
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectGenerationWhenVersionIsNull() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        SecurityPrincipal valid = createPrincipal();
        SecurityPrincipal principal = new SecurityPrincipal(valid.getUserId(), null, valid.getFullName(), valid.getUsername(), valid.getPassword(), valid.getRoleId(), valid.getRoleName(), valid.getBranchId(), valid.getBranchName(), true, valid.getAuthorities());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.generateToken(principal));

        /* Assert */
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectTokenWhenIssuerIsWrong() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.issuer("other");
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenAudienceIsWrong() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.audience().clear().add("other").and();
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenUseIsWrong() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.claim("token_use", "refresh");
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenTypeIsWrong() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.header().type("JWT");
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenIdIsMissing() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.id(null);
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenIdIsBlank() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.id(" ");
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenSubjectIsMissing() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.subject(null);
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenSubjectIsEmail() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.subject("ana@test.com");
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenSubjectIsZero() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.subject("0");
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenSubjectIsNegative() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.subject("-1");
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenSubjectHasLeadingZero() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.subject("01");
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenSubjectOverflowsLong() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.subject("9999999999999999999");
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenVersionIsMissing() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.claim("tv", null);
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenVersionIsZero() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.claim("tv", 0);
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenVersionIsNegative() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.claim("tv", -1);
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenIssuedAtIsMissing() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.issuedAt(null);
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenExpirationIsMissing() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.expiration(null);
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenExpirationPrecedesIssue() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.expiration(Date.from(createClock().instant().minusSeconds(1)));
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenLifetimeExceedsPolicy() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.expiration(Date.from(createClock().instant().plusSeconds(601)));
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenIssuedAtExceedsSkew() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.issuedAt(Date.from(createClock().instant().plusSeconds(31)));
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenTokenExpiredBeyondSkew() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        JwtBuilder builder = createTokenBuilder();
        builder.issuedAt(Date.from(createClock().instant().minusSeconds(632))).expiration(Date.from(createClock().instant().minusSeconds(32)));
        String token = builder.signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldAcceptTokenWhenExpirationIsWithinSkew() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        String token = createTokenBuilder().issuedAt(Date.from(createClock().instant().minusSeconds(610)))
                .expiration(Date.from(createClock().instant().minusSeconds(10))).signWith(createKey(), Jwts.SIG.HS256).compact();

        /* Act */
        JwtService.AccessToken result = service.parseAccessToken(token);

        /* Assert */
        assertThat(result).isEqualTo(new JwtService.AccessToken(1L, 1L));
    }

    @Test
    void shouldRejectTokenWhenSignatureIsWrong() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        String token = createTokenBuilder().signWith(Keys.hmacShaKeyFor(new byte[32]), Jwts.SIG.HS256).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenAlgorithmIsHs384() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        String token = createTokenBuilder().signWith(Keys.hmacShaKeyFor(new byte[48]), Jwts.SIG.HS384).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenAlgorithmIsHs512() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        String token = createTokenBuilder().signWith(Keys.hmacShaKeyFor(new byte[64]), Jwts.SIG.HS512).compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenUnsigned() throws Exception {
        /* Arrange */
        JwtService service = new JwtService(createProperties(), createClock());
        String token = createTokenBuilder().compact();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.parseAccessToken(token));

        /* Assert */
        assertThat(thrown).isInstanceOf(JwtException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {16, 31, 33, 64})
    void shouldRejectConfigurationWhenKeyLengthIsInvalid(int length) throws Exception {
        /* Arrange */
        JwtProperties properties = createProperties();
        properties.setSecret(Base64.getEncoder().encodeToString(new byte[length]));

        /* Act */
        Throwable thrown = catchThrowable(() -> new JwtService(properties, createClock()));

        /* Assert */
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    private SecurityPrincipal createPrincipal() {
        return new SecurityPrincipal(1L, 1L, "Ana Perez", "ana@test.com", "encoded-password",
                2L, "Operador", 3L, "Lima", true,
                List.of(new SimpleGrantedAuthority("security:users:read")));
    }

    private Clock createClock() {
        return Clock.fixed(Instant.parse("2026-09-18T12:00:00Z"), ZoneOffset.UTC);
    }

    private JwtProperties createProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(Base64.getEncoder()
                .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        properties.setIssuer("creditos-api");
        properties.setAudience("creditos-web");
        properties.setExpiration(600000L);
        properties.setClockSkewSeconds(30L);
        return properties;
    }

    private SecretKey createKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(createProperties().getSecret()));
    }

    private JwtBuilder createTokenBuilder() {
        return Jwts.builder().header().type("at+jwt").and().issuer("creditos-api")
                .audience().add("creditos-web").and().subject("1").id("test-token-id")
                .claim("tv", 1L).claim("token_use", "access")
                .issuedAt(Date.from(createClock().instant()))
                .expiration(Date.from(createClock().instant().plusSeconds(600)));
    }

}
