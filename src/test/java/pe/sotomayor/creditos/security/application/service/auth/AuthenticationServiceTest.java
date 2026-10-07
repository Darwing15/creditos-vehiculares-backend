package pe.sotomayor.creditos.security.application.service.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import pe.sotomayor.creditos.security.application.dto.auth.request.LoginRequest;
import pe.sotomayor.creditos.security.application.dto.auth.response.LoginResponse;

import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager manager;

    @Mock
    private JwtService jwt;

    @InjectMocks
    private AuthenticationService service;

    @Test
    void shouldReturnTokenAndIdentityWhenCredentialsAreValid() throws Exception {
        /* Arrange */
        LoginRequest request = createRequest();
        SecurityPrincipal principal = createPrincipal();
        Authentication authentication = createAuthentication(principal);
        when(manager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(jwt.generateToken(principal)).thenReturn("signed-token");
        when(jwt.expiresInSeconds()).thenReturn(600L);

        /* Act */
        LoginResponse result = service.authenticate(request);

        /* Assert */
        assertThat(result).isEqualTo(new LoginResponse(
                "signed-token",
                1L,
                "Ana Perez",
                "ana@test.com",
                2L,
                "Operador",
                3L,
                "Lima",
                "Bearer",
                600L)
        );
        ArgumentCaptor<Authentication> submitted = ArgumentCaptor.forClass(Authentication.class);
        verify(manager).authenticate(submitted.capture());
        assertThat(submitted.getValue().getName()).isEqualTo("ana@test.com");
        assertThat(submitted.getValue().getCredentials()).isEqualTo(request.password());
        assertThat(submitted.getValue().isAuthenticated()).isFalse();
        verify(jwt).generateToken(principal);
        verify(jwt).expiresInSeconds();
        verifyNoMoreInteractions(manager, jwt);
    }

    @Test
    void shouldPropagateFailureWhenCredentialsAreInvalid() throws Exception {
        /* Arrange */
        LoginRequest request = createRequest();
        BadCredentialsException failure = new BadCredentialsException("failure");
        when(manager.authenticate(any(Authentication.class))).thenThrow(failure);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.authenticate(request));

        /* Assert */
        assertThat(thrown).isSameAs(failure);
        verify(manager).authenticate(any(Authentication.class));
        verifyNoInteractions(jwt);
        verifyNoMoreInteractions(manager, jwt);
    }

    @Test
    void shouldPropagateFailureWhenAccountIsDisabled() throws Exception {
        /* Arrange */
        LoginRequest request = createRequest();
        DisabledException failure = new DisabledException("failure");
        when(manager.authenticate(any(Authentication.class))).thenThrow(failure);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.authenticate(request));

        /* Assert */
        assertThat(thrown).isSameAs(failure);
        verify(manager).authenticate(any(Authentication.class));
        verifyNoInteractions(jwt);
        verifyNoMoreInteractions(manager, jwt);
    }

    @Test
    void shouldPropagateFailureWhenProviderIsUnavailable() throws Exception {
        /* Arrange */
        LoginRequest request = createRequest();
        AuthenticationServiceException failure = new AuthenticationServiceException("failure");
        when(manager.authenticate(any(Authentication.class))).thenThrow(failure);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.authenticate(request));

        /* Assert */
        assertThat(thrown).isSameAs(failure);
        verify(manager).authenticate(any(Authentication.class));
        verifyNoInteractions(jwt);
        verifyNoMoreInteractions(manager, jwt);
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

    private LoginRequest createRequest() {
        return new LoginRequest(
                "  ANA@test.com  ",
                "  a valid password  "
        );
    }

    private Authentication createAuthentication(SecurityPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

}
