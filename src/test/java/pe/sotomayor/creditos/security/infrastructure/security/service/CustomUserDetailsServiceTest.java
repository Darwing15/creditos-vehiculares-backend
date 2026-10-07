package pe.sotomayor.creditos.security.infrastructure.security.service;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;
import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository users;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void shouldLoadPrincipalWhenIdExists() throws Exception {
        /* Arrange */
        User user = createUser();
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getTokenVersion()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo(user.getEmail());
        assertThat(result.getFullName()).isEqualTo(user.getFullName());
        assertThat(result.getRoleId()).isEqualTo(2L);
        assertThat(result.getRoleName()).isEqualTo("Operador");
        assertThat(result.getBranchId()).isEqualTo(3L);
        assertThat(result.getBranchName()).isEqualTo("Lima");
        assertThat(result.getPassword()).isEqualTo(user.getPassword());
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("security:users:read");
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldLoadPrincipalWhenEmailNeedsNormalization() throws Exception {
        /* Arrange */
        User user = createUser();
        when(users.findForAuthenticationByEmail("ana@test.com")).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserByUsername("  ANA@TEST.COM  ");

        /* Assert */
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getTokenVersion()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo(user.getEmail());
        assertThat(result.getFullName()).isEqualTo(user.getFullName());
        assertThat(result.getRoleId()).isEqualTo(2L);
        assertThat(result.getRoleName()).isEqualTo("Operador");
        assertThat(result.getBranchId()).isEqualTo(3L);
        assertThat(result.getBranchName()).isEqualTo("Lima");
        assertThat(result.getPassword()).isEqualTo(user.getPassword());
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("security:users:read");
        verify(users).findForAuthenticationByEmail("ana@test.com");
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldThrowNotFoundWhenIdDoesNotExist() throws Exception {
        /* Arrange */
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.loadUserById(1L));

        /* Assert */
        assertThat(thrown).isInstanceOf(UsernameNotFoundException.class);
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldThrowNotFoundWhenEmailDoesNotExist() throws Exception {
        /* Arrange */
        when(users.findForAuthenticationByEmail("ana@test.com")).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.loadUserByUsername("ana@test.com"));

        /* Assert */
        assertThat(thrown).isInstanceOf(UsernameNotFoundException.class);
        verify(users).findForAuthenticationByEmail("ana@test.com");
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldRejectEmailWhenNull() throws Exception {
        /* Arrange */
        String email = null;

        /* Act */
        Throwable thrown = catchThrowable(() -> service.loadUserByUsername(email));

        /* Assert */
        assertThat(thrown).isInstanceOf(UsernameNotFoundException.class);
        verifyNoInteractions(users);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldDisablePrincipalWhenUserIsInactive() throws Exception {
        /* Arrange */
        User user = createUser();
        user.setActive(false);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.isEnabled()).isFalse();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldDisablePrincipalWhenUserIsDeleted() throws Exception {
        /* Arrange */
        User user = createUser();
        user.setDeleted(true);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.isEnabled()).isFalse();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldDisablePrincipalWhenRoleIsDeleted() throws Exception {
        /* Arrange */
        User user = createUser();
        user.getRole().setDeleted(true);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.isEnabled()).isFalse();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldDisablePrincipalWhenBranchIsInactive() throws Exception {
        /* Arrange */
        User user = createUser();
        user.getBranch().setActive(false);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.isEnabled()).isFalse();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldDisablePrincipalWhenBranchIsDeleted() throws Exception {
        /* Arrange */
        User user = createUser();
        user.getBranch().setDeleted(true);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.isEnabled()).isFalse();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldDisablePrincipalWhenVersionIsNull() throws Exception {
        /* Arrange */
        User user = createUser();
        user.setTokenVersion(null);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.isEnabled()).isFalse();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldDisablePrincipalWhenVersionIsZero() throws Exception {
        /* Arrange */
        User user = createUser();
        user.setTokenVersion(0L);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.isEnabled()).isFalse();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldDisablePrincipalWhenVersionIsNegative() throws Exception {
        /* Arrange */
        User user = createUser();
        user.setTokenVersion(-1L);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.isEnabled()).isFalse();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldDisablePrincipalWhenUserActiveIsNull() throws Exception {
        /* Arrange */
        User user = createUser();
        user.setActive(null);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.isEnabled()).isFalse();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldDisablePrincipalWhenUserDeletedIsNull() throws Exception {
        /* Arrange */
        User user = createUser();
        user.setDeleted(null);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.isEnabled()).isFalse();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldDisablePrincipalWhenRoleDeletedIsNull() throws Exception {
        /* Arrange */
        User user = createUser();
        user.getRole().setDeleted(null);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.isEnabled()).isFalse();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldDisablePrincipalWhenBranchActiveIsNull() throws Exception {
        /* Arrange */
        User user = createUser();
        user.getBranch().setActive(null);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.isEnabled()).isFalse();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldDisablePrincipalWhenBranchDeletedIsNull() throws Exception {
        /* Arrange */
        User user = createUser();
        user.getBranch().setDeleted(null);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.isEnabled()).isFalse();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldRejectPrincipalWhenRoleIsMissing() throws Exception {
        /* Arrange */
        User user = createUser();
        user.setRole(null);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.loadUserById(1L));

        /* Assert */
        assertThat(thrown).isInstanceOf(UsernameNotFoundException.class);
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldRejectPrincipalWhenBranchIsMissing() throws Exception {
        /* Arrange */
        User user = createUser();
        user.setBranch(null);
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.loadUserById(1L));

        /* Assert */
        assertThat(thrown).isInstanceOf(UsernameNotFoundException.class);
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void shouldGrantNoPermissionsWhenRoleOnlyHasAdminName() throws Exception {
        /* Arrange */
        User user = createUser();
        user.getRole().setName("ADMINISTRADOR");
        user.getRole().getPermissions().clear();
        when(users.findForAuthenticationById(1L)).thenReturn(Optional.of(user));

        /* Act */
        SecurityPrincipal result = service.loadUserById(1L);

        /* Assert */
        assertThat(result.getAuthorities()).isEmpty();
        verify(users).findForAuthenticationById(1L);
        verifyNoMoreInteractions(users);
    }

    private User createUser() {
        return User.builder()
                .id(1L)
                .fullName("Ana Perez")
                .email("ana@test.com")
                .password("encoded-password")
                .active(true).deleted(false)
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
