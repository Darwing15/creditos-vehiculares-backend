package pe.sotomayor.creditos.security.application.service.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import pe.sotomayor.creditos.security.application.dto.auth.response.AuthenticatedUserResponse;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;
import pe.sotomayor.creditos.security.infrastructure.security.service.CurrentSecurityPrincipal;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAuthenticatedUserServiceTest {

    @Mock
    private CurrentSecurityPrincipal current;

    @InjectMocks
    private GetAuthenticatedUserService service;

    @Test
    void shouldReturnIdentityAndSortedPermissionsWhenPrincipalExists() throws Exception {
        /* Arrange */
        SecurityPrincipal principal = createPrincipal();
        when(current.get()).thenReturn(principal);

        /* Act */
        AuthenticatedUserResponse result = service.getCurrentUser();

        /* Assert */
        assertThat(result).isEqualTo(new AuthenticatedUserResponse(
                1L,
                "Ana Perez",
                "ana@test.com",
                2L,
                "Operador",
                3L,
                "Lima",
                List.of("security:roles:read", "security:users:read"))
        );
        verify(current).get();
        verifyNoMoreInteractions(current);
    }

    @Test
    void shouldPropagateFailureWhenAuthenticationIsAbsent() throws Exception {
        /* Arrange */
        when(current.get()).thenThrow(new BadCredentialsException("missing"));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.getCurrentUser());

        /* Assert */
        assertThat(thrown).isInstanceOf(BadCredentialsException.class);
        verify(current).get();
        verifyNoMoreInteractions(current);
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
                List.of(
                        new SimpleGrantedAuthority("security:users:read"),
                        new SimpleGrantedAuthority("security:roles:read")
                )
        );
    }

}
