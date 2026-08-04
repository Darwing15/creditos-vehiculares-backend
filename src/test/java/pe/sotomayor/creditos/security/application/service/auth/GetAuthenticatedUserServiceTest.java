package pe.sotomayor.creditos.security.application.service.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import pe.sotomayor.creditos.security.application.dto.auth.response.AuthenticatedUserResponse;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.UnauthorizedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAuthenticatedUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetAuthenticatedUserService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnAuthenticatedUser() {

        /* Arrange */

        Authentication authentication = createAuthentication();
        User user = createUser();

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        when(userRepository.findUserWithRoleAndBranchByEmail(
                authentication.getName()))
                .thenReturn(Optional.of(user));

        /* Act */

        AuthenticatedUserResponse response =
                service.getCurrentUser();

        /* Assert */

        assertThat(response).isNotNull();

        assertThat(response.id())
                .isEqualTo(user.getId());

        assertThat(response.fullName())
                .isEqualTo(user.getFullName());

        assertThat(response.email())
                .isEqualTo(user.getEmail());

        assertThat(response.roleId())
                .isEqualTo(user.getRole().getId());

        assertThat(response.roleName())
                .isEqualTo(user.getRole().getName());

        assertThat(response.branchId())
                .isEqualTo(user.getBranch().getId());

        assertThat(response.branchName())
                .isEqualTo(user.getBranch().getName());

        verify(userRepository)
                .findUserWithRoleAndBranchByEmail(
                        authentication.getName());

        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenAuthenticationIsNull() {

        /* Arrange */

        SecurityContextHolder.clearContext();

        /* Act */

        Throwable exception =
                catchThrowable(() -> service.getCurrentUser());

        /* Assert */

        assertThat(exception)
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenAuthenticationIsAnonymous() {

        /* Arrange */

        Authentication authentication = createAnonymousAuthentication();

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        /* Act */

        Throwable exception =
                catchThrowable(() -> service.getCurrentUser());

        /* Assert */

        assertThat(exception)
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserDoesNotExist() {

        /* Arrange */

        Authentication authentication = createAuthentication();

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        when(userRepository.findUserWithRoleAndBranchByEmail(
                authentication.getName()))
                .thenReturn(Optional.empty());

        /* Act */

        Throwable exception =
                catchThrowable(() -> service.getCurrentUser());

        /* Assert */

        assertThat(exception)
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userRepository)
                .findUserWithRoleAndBranchByEmail(
                        authentication.getName());

        verifyNoMoreInteractions(userRepository);
    }

    private Authentication createAuthentication() {

        return new UsernamePasswordAuthenticationToken(
                "darwin@creditos.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private Authentication createAnonymousAuthentication() {

        return new AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                List.of(
                        new SimpleGrantedAuthority("ROLE_ANONYMOUS")
                )
        );
    }

    private User createUser() {

        User user = new User();

        user.setId(1L);
        user.setFullName("Darwin G.");
        user.setEmail("darwin@creditos.com");
        user.setRole(createRole());
        user.setBranch(createBranch());

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
        branch.setName("Sede Tocache");

        return branch;
    }

}
