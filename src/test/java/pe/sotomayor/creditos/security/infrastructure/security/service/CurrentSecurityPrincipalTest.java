package pe.sotomayor.creditos.security.infrastructure.security.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CurrentSecurityPrincipalTest {
    private final CurrentSecurityPrincipal service = new CurrentSecurityPrincipal();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnPrincipalWhenAuthenticationIsValid() throws Exception {
        /* Arrange */
        SecurityPrincipal principal = createPrincipal();
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(principal));

        /* Act */
        SecurityPrincipal result = service.get();

        /* Assert */
        assertThat(result).isSameAs(principal);
    }

    @Test
    void shouldRejectAuthenticationWhenMissing() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(null);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.get());

        /* Assert */
        assertThat(thrown).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldRejectAuthenticationWhenUnauthenticated() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.unauthenticated(createPrincipal(), null));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.get());

        /* Assert */
        assertThat(thrown).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldRejectAuthenticationWhenDifferentPrincipal() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("ana@test.com", null, List.of()));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.get());

        /* Assert */
        assertThat(thrown).isInstanceOf(BadCredentialsException.class);
    }

    private SecurityPrincipal createPrincipal() {
        return new SecurityPrincipal(
                1L,
                1L,
                "Ana Perez",
                "ana@test.com",
                "encoded-password",
                2L,
                "Operador",
                3L,
                "Lima",
                true,
                List.of(new SimpleGrantedAuthority("security:users:read"))
        );
    }

    private Authentication createAuthentication(SecurityPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
