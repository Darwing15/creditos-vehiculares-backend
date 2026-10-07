package pe.sotomayor.creditos.security.application.service.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import pe.sotomayor.creditos.security.application.dto.auth.request.ChangePasswordRequest;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;
import pe.sotomayor.creditos.security.infrastructure.security.service.CurrentSecurityPrincipal;
import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.PasswordReuseException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private CurrentSecurityPrincipal current;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private ChangePasswordService service;

    @Test
    void shouldRevokeExistingTokensWhenSessionIsValid() throws Exception {
        /* Arrange */
        ChangePasswordRequest request = createRequest();
        SecurityPrincipal principal = createPrincipal();
        User user = createUser();
        when(current.get()).thenReturn(principal);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));
        when(encoder.matches(request.currentPassword(), user.getPassword())).thenReturn(true);
        when(encoder.encode(request.newPassword())).thenReturn("new-encoded-password");

        /* Act */
        service.changePassword(request);

        /* Assert */
        assertThat(user.getTokenVersion()).isEqualTo(2L);
        assertThat(user.getPassword()).isEqualTo("new-encoded-password");
        verify(current).get();
        verify(users).findForAuthenticationById(1L);
        verify(encoder).matches(request.currentPassword(), "encoded-password");
        verify(encoder).matches(request.newPassword(), "encoded-password");
        verify(encoder).encode(request.newPassword());
        verify(users).saveAndFlush(user);
        verifyNoMoreInteractions(users, current, encoder);
    }

    @Test
    void shouldRejectSessionWhenUserDoesNotExist() throws Exception {
        /* Arrange */
        ChangePasswordRequest request = createRequest();
        when(current.get()).thenReturn(createPrincipal());
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.changePassword(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(BadCredentialsException.class);
        verify(current).get();
        verify(users).findForAuthenticationById(1L);
        verifyNoInteractions(encoder);
        verifyNoMoreInteractions(users, current, encoder);
    }

    @Test
    void shouldRejectSessionWhenTokenVersionHasChanged() throws Exception {
        /* Arrange */
        ChangePasswordRequest request = createRequest();
        SecurityPrincipal principal = createPrincipal();
        User user = createUser();
        when(current.get()).thenReturn(principal);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));
        user.setTokenVersion(2L);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.changePassword(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(BadCredentialsException.class);
        verify(current).get();
        verify(users).findForAuthenticationById(1L);
        verifyNoInteractions(encoder);
        verifyNoMoreInteractions(users, current, encoder);
    }

    @Test
    void shouldRejectPasswordChangeWhenCurrentPasswordIsIncorrect() throws Exception {
        /* Arrange */
        ChangePasswordRequest request = createRequest();
        SecurityPrincipal principal = createPrincipal();
        User user = createUser();
        when(current.get()).thenReturn(principal);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.changePassword(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(BadCredentialsException.class);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(current).get();
        verify(users).findForAuthenticationById(1L);
        verify(encoder).matches(request.currentPassword(), "encoded-password");
        verifyNoMoreInteractions(users, current, encoder);
    }

    @Test
    void shouldRejectPasswordChangeWhenNewPasswordIsReused() throws Exception {
        /* Arrange */
        ChangePasswordRequest request = createRequest();
        SecurityPrincipal principal = createPrincipal();
        User user = createUser();
        when(current.get()).thenReturn(principal);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));
        when(encoder.matches(request.currentPassword(), user.getPassword())).thenReturn(true);
        when(encoder.matches(request.newPassword(), user.getPassword())).thenReturn(true);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.changePassword(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(PasswordReuseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NEW_PASSWORD_MUST_BE_DIFFERENT);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(current).get();
        verify(users).findForAuthenticationById(1L);
        verify(encoder).matches(request.currentPassword(), "encoded-password");
        verify(encoder).matches(request.newPassword(), "encoded-password");
        verifyNoMoreInteractions(users, current, encoder);
    }

    @Test
    void shouldRejectPasswordChangeWhenNewPasswordExceedsBcryptBytes() throws Exception {
        /* Arrange */
        ChangePasswordRequest valid = createRequest();
        ChangePasswordRequest request = new ChangePasswordRequest(valid.currentPassword(), "á".repeat(37));
        SecurityPrincipal principal = createPrincipal();
        User user = createUser();
        when(current.get()).thenReturn(principal);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));
        when(encoder.matches(request.currentPassword(), user.getPassword())).thenReturn(true);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.changePassword(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(current).get();
        verify(users).findForAuthenticationById(1L);
        verify(encoder).matches(request.currentPassword(), "encoded-password");
        verifyNoMoreInteractions(users, current, encoder);
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

    private User createUser() {
        return User.builder()
                .id(1L)
                .fullName("Ana Perez")
                .email("ana@test.com")
                .password("encoded-password")
                .active(true)
                .deleted(false)
                .tokenVersion(1L)
                .role(createRole())
                .branch(createBranch())
                .build();
    }

    private Role createRole() {
        return Role.builder()
                .id(2L)
                .name("Operador")
                .description("Consulta de usuarios")
                .permissions(new HashSet<>(Set.of("security:users:read")))
                .deleted(false)
                .build();
    }

    private Branch createBranch() {
        return Branch.builder()
                .id(3L)
                .code("LIM")
                .name("Lima")
                .address("Av. Peru 123")
                .email("lima@test.com")
                .phone("999999999")
                .active(true)
                .deleted(false)
                .build();
    }

    private ChangePasswordRequest createRequest() {
        return new ChangePasswordRequest("current password", "new strong password");
    }

}
