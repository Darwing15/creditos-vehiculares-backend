package pe.sotomayor.creditos.security.application.service.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import pe.sotomayor.creditos.security.application.dto.auth.request.LoginRequest;
import pe.sotomayor.creditos.security.application.dto.auth.response.LoginResponse;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void shouldAuthenticateUserWhenCredentialsAreValid() {

        /* Arrange */

        LoginRequest request = createRequest();

        Authentication authentication = createAuthentication();

        User user = createUser();

        when(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(userRepository.findUserWithRoleAndBranchByEmail(
                request.email()))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(user))
                .thenReturn("jwt-token");

        /* Act */

        LoginResponse response =
                authenticationService.authenticate(request);

        /* Assert */

        assertThat(response).isNotNull();

        assertThat(response.token())
                .isEqualTo("jwt-token");

        assertThat(response.userId())
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

        verify(authenticationManager)
                .authenticate(any(
                        UsernamePasswordAuthenticationToken.class));

        verify(userRepository)
                .findUserWithRoleAndBranchByEmail(request.email());

        verify(jwtService)
                .generateToken(user);

        verifyNoMoreInteractions(
                authenticationManager,
                userRepository,
                jwtService);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenAuthenticatedUserDoesNotExist() {

        /* Arrange */

        LoginRequest request = createRequest();

        Authentication authentication = createAuthentication();

        when(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(userRepository.findUserWithRoleAndBranchByEmail(
                request.email()))
                .thenReturn(Optional.empty());

        /* Act */

        Throwable exception = catchThrowable(() ->
                authenticationService.authenticate(request));

        /* Assert */

        assertThat(exception)
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(authenticationManager)
                .authenticate(any(
                        UsernamePasswordAuthenticationToken.class));

        verify(userRepository)
                .findUserWithRoleAndBranchByEmail(request.email());

        verifyNoInteractions(jwtService);

        verifyNoMoreInteractions(
                authenticationManager,
                userRepository);
    }

    @Test
    void shouldGenerateJwtForAuthenticatedUser() {

        /* Arrange */

        LoginRequest request = createRequest();

        Authentication authentication = createAuthentication();

        User user = createUser();

        when(authenticationManager.authenticate(any(
                UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(userRepository.findUserWithRoleAndBranchByEmail(
                request.email()))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(user))
                .thenReturn("jwt-token");

        /* Act */

        authenticationService.authenticate(request);

        /* Assert */

        verify(jwtService)
                .generateToken(user);
    }

    private Authentication createAuthentication() {

        return new UsernamePasswordAuthenticationToken(
                "darwin@creditos.com",
                null
        );
    }

    private LoginRequest createRequest() {

        return new LoginRequest(
                "darwin@creditos.com",
                "Password123"
        );
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
        user.setTokenVersion(1L);

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
