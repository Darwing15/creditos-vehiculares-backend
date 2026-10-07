package pe.sotomayor.creditos.security.infrastructure.security.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityCurrentUserProviderTest {

    @Mock
    private CurrentSecurityPrincipal current;

    @InjectMocks
    private SecurityCurrentUserProvider service;

    @Test
    void shouldReturnStableIdWhenPrincipalExists() throws Exception {
        /* Arrange */
        when(current.get()).thenReturn(createPrincipal());

        /* Act */
        Long result = service.getCurrentUserId();

        /* Assert */
        assertThat(result).isEqualTo(1L);
        verify(current).get();
        verifyNoMoreInteractions(current);
    }

    @Test
    void shouldPropagateFailureWhenPrincipalIsUnavailable() throws Exception {
        /* Arrange */
        when(current.get()).thenThrow(new BadCredentialsException("missing"));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.getCurrentUserId());

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
                List.of(new SimpleGrantedAuthority("security:users:read"))
        );
    }

}
