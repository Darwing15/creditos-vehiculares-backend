package pe.sotomayor.creditos.security.infrastructure.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {

        customUserDetailsService =
                new CustomUserDetailsService(userRepository);

    }

    @Test
    void shouldLoadUserDetailsWhenUserExists() {

        /* Arrange */

        User user = createUser();

        when(userRepository.findUserWithRoleAndBranchByEmail(
                user.getEmail()))
                .thenReturn(Optional.of(user));

        /* Act */

        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername(
                        user.getEmail());

        /* Assert */

        assertThat(userDetails.getUsername())
                .isEqualTo(user.getEmail());

        assertThat(userDetails.getPassword())
                .isEqualTo(user.getPassword());

        assertThat(userDetails.isEnabled())
                .isTrue();

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMINISTRADOR");

        verify(userRepository)
                .findUserWithRoleAndBranchByEmail(user.getEmail());

        verifyNoMoreInteractions(userRepository);

    }

    @Test
    void shouldReturnDisabledUserDetailsWhenUserIsInactive() {

        /* Arrange */

        User user = createUser();

        user.setActive(false);

        when(userRepository.findUserWithRoleAndBranchByEmail(
                user.getEmail()))
                .thenReturn(Optional.of(user));

        /* Act */

        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername(
                        user.getEmail());

        /* Assert */

        assertThat(userDetails.isEnabled())
                .isFalse();

        verify(userRepository)
                .findUserWithRoleAndBranchByEmail(user.getEmail());

        verifyNoMoreInteractions(userRepository);

    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        /* Arrange */

        String email = "darwin@creditos.com";

        when(userRepository.findUserWithRoleAndBranchByEmail(email))
                .thenReturn(Optional.empty());

        /* Act & Assert */

        assertThatThrownBy(() ->
                customUserDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: " + email);

        verify(userRepository)
                .findUserWithRoleAndBranchByEmail(email);

        verifyNoMoreInteractions(userRepository);

    }

    @Test
    void shouldLoadDomainUserWhenUserExists() {

        /* Arrange */

        User user = createUser();

        when(userRepository.findUserWithRoleAndBranchByEmail(
                user.getEmail()))
                .thenReturn(Optional.of(user));

        /* Act */

        User domainUser =
                customUserDetailsService.loadDomainUser(
                        user.getEmail());

        /* Assert */

        assertThat(domainUser)
                .isSameAs(user);

        verify(userRepository)
                .findUserWithRoleAndBranchByEmail(user.getEmail());

        verifyNoMoreInteractions(userRepository);

    }

    @Test
    void shouldThrowExceptionWhenDomainUserDoesNotExist() {

        /* Arrange */

        String email = "darwin@creditos.com";

        when(userRepository.findUserWithRoleAndBranchByEmail(email))
                .thenReturn(Optional.empty());

        /* Act & Assert */

        assertThatThrownBy(() ->
                customUserDetailsService.loadDomainUser(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: " + email);

        verify(userRepository)
                .findUserWithRoleAndBranchByEmail(email);

        verifyNoMoreInteractions(userRepository);

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
