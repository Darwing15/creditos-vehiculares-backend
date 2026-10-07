package pe.sotomayor.creditos.security.application.service.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;
import pe.sotomayor.creditos.security.infrastructure.security.service.CurrentSecurityPrincipal;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutAllServiceTest {
    @Mock
    private UserRepository users;

    @Mock
    private CurrentSecurityPrincipal current;

    @InjectMocks
    private LogoutAllService service;

    @Test
    void shouldRevokeExistingTokensWhenSessionIsValid() throws Exception {
        /* Arrange */
        SecurityPrincipal principal = createPrincipal();
        User user = createUser();
        when(current.get()).thenReturn(principal);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        service.logoutAll();

        /* Assert */
        assertThat(user.getTokenVersion()).isEqualTo(2L);
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(current).get();
        verify(users).findForAuthenticationById(1L);
        verify(users).saveAndFlush(user);
        verifyNoMoreInteractions(users, current);
    }

    @Test
    void shouldRejectSessionWhenUserDoesNotExist() throws Exception {
        /* Arrange */
        when(current.get()).thenReturn(createPrincipal());
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.logoutAll());

        /* Assert */
        assertThat(thrown).isInstanceOf(BadCredentialsException.class);
        verify(current).get();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users, current);
    }

    @Test
    void shouldRejectSessionWhenTokenVersionHasChanged() throws Exception {
        /* Arrange */
        SecurityPrincipal principal = createPrincipal();
        User user = createUser();
        when(current.get()).thenReturn(principal);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));
        user.setTokenVersion(2L);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.logoutAll());

        /* Assert */
        assertThat(thrown).isInstanceOf(BadCredentialsException.class);
        verify(current).get();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users, current);
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

}

