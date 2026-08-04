package pe.sotomayor.creditos.security.application.service.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.crypto.password.PasswordEncoder;

import pe.sotomayor.creditos.security.application.dto.auth.request.ChangePasswordRequest;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.PasswordReuseException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.UnauthorizedException;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ChangePasswordService service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldChangePasswordWhenCurrentPasswordIsValid() {

        /* Arrange */

        Authentication authentication = createAuthentication();

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        ChangePasswordRequest request = createRequest();

        User user = createUser();

        when(userRepository.findUserWithRoleAndBranchByEmail(
                user.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.currentPassword(),
                user.getPassword()))
                .thenReturn(true);

        when(passwordEncoder.matches(
                request.newPassword(),
                user.getPassword()))
                .thenReturn(false);

        when(passwordEncoder.encode(
                request.newPassword()))
                .thenReturn("encoded-password");

        /* Act */

        service.changePassword(request);

        /* Assert */

        assertThat(user.getPassword())
                .isEqualTo("encoded-password");

        assertThat(user.getTokenVersion())
                .isEqualTo(2L);

        verify(userRepository)
                .save(user);

        verifyNoMoreInteractions(
                userRepository,
                passwordEncoder);
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenUserIsNotAuthenticated() {

        /* Arrange */

        ChangePasswordRequest request = createRequest();

        /* Act */

        Throwable exception = catchThrowable(() ->
                service.changePassword(request));

        /* Assert */

        assertThat(exception)
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verifyNoMoreInteractions(
                userRepository,
                passwordEncoder);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserDoesNotExist() {

        /* Arrange */

        Authentication authentication = createAuthentication();

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        ChangePasswordRequest request = createRequest();

        when(userRepository.findUserWithRoleAndBranchByEmail(
                authentication.getName()))
                .thenReturn(Optional.empty());

        /* Act */

        Throwable exception = catchThrowable(() ->
                service.changePassword(request));

        /* Assert */

        assertThat(exception)
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verifyNoMoreInteractions(
                userRepository,
                passwordEncoder);
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenUserIsInactive() {

        /* Arrange */

        Authentication authentication = createAuthentication();

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        ChangePasswordRequest request = createRequest();

        User user = createUser();
        user.setActive(false);

        when(userRepository.findUserWithRoleAndBranchByEmail(
                authentication.getName()))
                .thenReturn(Optional.of(user));

        /* Act */

        Throwable exception = catchThrowable(() ->
                service.changePassword(request));

        /* Assert */

        assertThat(exception)
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(passwordEncoder, never())
                .matches(request.currentPassword(), user.getPassword());

        verifyNoMoreInteractions(
                userRepository,
                passwordEncoder);
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenCurrentPasswordIsIncorrect() {

        /* Arrange */

        Authentication authentication = createAuthentication();

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        ChangePasswordRequest request = createRequest();

        User user = createUser();

        when(userRepository.findUserWithRoleAndBranchByEmail(
                authentication.getName()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.currentPassword(),
                user.getPassword()))
                .thenReturn(false);

        /* Act */

        Throwable exception = catchThrowable(() ->
                service.changePassword(request));

        /* Assert */

        assertThat(exception)
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(passwordEncoder, never())
                .encode(request.newPassword());

        verify(userRepository, never())
                .save(user);

        verifyNoMoreInteractions(
                userRepository,
                passwordEncoder);
    }

    @Test
    void shouldThrowPasswordReuseExceptionWhenNewPasswordMatchesCurrentPassword() {

        /* Arrange */

        Authentication authentication = createAuthentication();

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        ChangePasswordRequest request = createRequest();

        User user = createUser();

        when(userRepository.findUserWithRoleAndBranchByEmail(
                authentication.getName()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.currentPassword(),
                user.getPassword()))
                .thenReturn(true);

        when(passwordEncoder.matches(
                request.newPassword(),
                user.getPassword()))
                .thenReturn(true);

        /* Act */

        Throwable exception = catchThrowable(() ->
                service.changePassword(request));

        /* Assert */

        assertThat(exception)
                .isInstanceOf(PasswordReuseException.class);

        verify(passwordEncoder, never())
                .encode(request.newPassword());

        verify(userRepository, never())
                .save(user);

        verifyNoMoreInteractions(
                userRepository,
                passwordEncoder);
    }

    @Test
    void shouldIncreaseTokenVersionWhenPasswordChanges() {

        /* Arrange */

        Authentication authentication = createAuthentication();

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        ChangePasswordRequest request = createRequest();

        User user = createUser();

        when(userRepository.findUserWithRoleAndBranchByEmail(
                authentication.getName()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.currentPassword(),
                user.getPassword()))
                .thenReturn(true);

        when(passwordEncoder.matches(
                request.newPassword(),
                user.getPassword()))
                .thenReturn(false);

        when(passwordEncoder.encode(
                request.newPassword()))
                .thenReturn("encoded");

        /* Act */

        service.changePassword(request);

        /* Assert */

        assertThat(user.getTokenVersion())
                .isEqualTo(2L);
    }

    private Authentication createAuthentication() {

        return new UsernamePasswordAuthenticationToken(
                "darwin@creditos.com",
                null,
                Collections.emptyList()
        );
    }

    private ChangePasswordRequest createRequest() {

        return new ChangePasswordRequest(
                "Password123",
                "NewPassword123"
        );
    }

    private User createUser() {

        User user = new User();

        user.setId(1L);
        user.setFullName("Darwin G.");
        user.setEmail("darwin@creditos.com");
        user.setPassword("$2a$password");
        user.setActive(true);
        user.setTokenVersion(1L);
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
