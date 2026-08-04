package pe.sotomayor.creditos.security.infrastructure.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {

        JwtProperties properties = new JwtProperties();

        properties.setSecret(
                "1234567890123456789012345678901234567890123456789012345678901234");

        properties.setExpiration(3600000L);

        jwtService = new JwtService(properties);

    }

    @Test
    void shouldGenerateValidJwtToken() {

        /* Arrange */

        User user = createUser();

        /* Act */

        String token = generateToken(user);

        /* Assert */

        assertThat(token)
                .isNotBlank();

        assertThat(jwtService.isTokenValid(token))
                .isTrue();

    }

    @Test
    void shouldExtractUsernameFromToken() {

        /* Arrange */

        User user = createUser();

        String token = generateToken(user);

        /* Act */

        String username =
                jwtService.extractUsername(token);

        /* Assert */

        assertThat(username)
                .isEqualTo(user.getEmail());

    }

    @Test
    void shouldExtractTokenVersionFromToken() {

        /* Arrange */

        User user = createUser();

        String token = generateToken(user);

        /* Act */

        Long tokenVersion =
                jwtService.extractTokenVersion(token);

        /* Assert */

        assertThat(tokenVersion)
                .isEqualTo(user.getTokenVersion());
    }

    @Test
    void shouldReturnFalseWhenTokenIsInvalid() {

        /* Arrange */

        String token = "invalid.jwt.token";

        /* Act */

        boolean valid =
                jwtService.isTokenValid(token);

        /* Assert */

        assertThat(valid)
                .isFalse();

    }

    @Test
    void shouldGenerateDifferentTokensForSameUser() {

        /* Arrange */

        User user = createUser();

        /* Act */

        String token1 =
                generateToken(user);

        String token2 =
                generateToken(user);

        /* Assert */

        assertThat(token1)
                .isNotEqualTo(token2);

        assertThat(jwtService.isTokenValid(token1))
                .isTrue();

        assertThat(jwtService.isTokenValid(token2))
                .isTrue();

    }

    @Test
    void shouldStoreCurrentTokenVersionInsideJwt() {

        /* Arrange */

        User user = createUser();

        user.setTokenVersion(8L);

        /* Act */

        String token = generateToken(user);

        /* Assert */

        assertThat(jwtService.extractTokenVersion(token))
                .isEqualTo(8L);
    }

    private String generateToken(User user) {

        return jwtService.generateToken(user);
    }

    private User createUser() {

        User user = new User();

        user.setId(1L);
        user.setFullName("Darwin G.");
        user.setEmail("darwin@creditos.com");
        user.setPassword("$2a$10$passwordHash");
        user.setRole(createRole());
        user.setBranch(createBranch());
        user.setActive(true);
        user.setTokenVersion(3L);

        return user;
    }

    private Role createRole() {

        Role role = new Role();

        role.setId(1L);
        role.setName("Administrador");

        return role;
    }

    private Branch createBranch() {

        Branch branch = new Branch();

        branch.setId(1L);
        branch.setName("Sucursal Tocache");

        return branch;
    }

}
