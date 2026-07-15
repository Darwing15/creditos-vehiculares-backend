package pe.sotomayor.creditos.security.application.service.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import pe.sotomayor.creditos.security.application.dto.auth.request.LoginRequest;
import pe.sotomayor.creditos.security.application.dto.auth.response.LoginResponse;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.UnauthorizedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link AuthenticationService}.
 *
 * <p>
 * Verifica el comportamiento del caso de uso encargado de autenticar
 * usuarios mediante correo electrónico y contraseña.
 * </p>
 *
 * @author DarwinG
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("Debe autenticar un usuario con credenciales válidas")
    void shouldAuthenticateUserSuccessfully() {

        /* Arrange */

        LoginRequest request = createRequest();

        User user = createUser();

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.password(),
                user.getPassword()))
                .thenReturn(true);

        /* Act */

        LoginResponse response =
                authenticationService.authenticate(request);

        /* Assert */

        assertNotNull(response);

        assertEquals(
                1L,
                response.userId()
        );

        assertEquals(
                "Darwin G.",
                response.fullName()
        );

        assertEquals(
                "darwin@creditos.com",
                response.email()
        );

        assertEquals(
                1L,
                response.roleId()
        );

        assertEquals(
                "Administrador",
                response.roleName()
        );

        assertEquals(
                1L,
                response.branchId()
        );

        assertEquals(
                "Sucursal Tocache",
                response.branchName()
        );

        verify(userRepository)
                .findByEmail(request.email());

        verify(passwordEncoder)
                .matches(request.password(), user.getPassword());

        verifyNoMoreInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el correo no existe")
    void shouldThrowExceptionWhenUserDoesNotExist() {

        /* Arrange */

        LoginRequest request = createRequest();

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        /* Act & Assert */

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () -> authenticationService.authenticate(request)
                );

        assertEquals(
                ErrorCode.INVALID_CREDENTIALS,
                exception.getErrorCode()
        );

        verify(userRepository)
                .findByEmail(request.email());

        verifyNoMoreInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el usuario está inactivo")
    void shouldThrowExceptionWhenUserIsInactive() {

        /* Arrange */

        LoginRequest request = createRequest();

        User user = createUser();

        user.setActive(false);

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        /* Act & Assert */

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () -> authenticationService.authenticate(request)
                );

        assertEquals(
                ErrorCode.INVALID_CREDENTIALS,
                exception.getErrorCode()
        );

        verify(userRepository)
                .findByEmail(request.email());

        verifyNoMoreInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la contraseña es incorrecta")
    void shouldThrowExceptionWhenPasswordIsInvalid() {

        /* Arrange */

        LoginRequest request = createRequest();

        User user = createUser();

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.password(),
                user.getPassword()))
                .thenReturn(false);

        /* Act & Assert */

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () -> authenticationService.authenticate(request)
                );

        assertEquals(
                ErrorCode.INVALID_CREDENTIALS,
                exception.getErrorCode()
        );

        verify(userRepository)
                .findByEmail(request.email());

        verify(passwordEncoder)
                .matches(request.password(), user.getPassword());

        verifyNoMoreInteractions(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Debe validar la contraseña utilizando PasswordEncoder")
    void shouldValidatePasswordUsingPasswordEncoder() {

        /* Arrange */

        LoginRequest request = createRequest();

        User user = createUser();

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.password(),
                user.getPassword()))
                .thenReturn(true);

        /* Act */

        authenticationService.authenticate(request);

        /* Assert */

        verify(passwordEncoder)
                .matches(
                        request.password(),
                        user.getPassword()
                );
    }

    private LoginRequest createRequest() {

        return new LoginRequest(
                "darwin@creditos.com",
                "Password123"
        );
    }

    private User createUser() {

        Role role = new Role();
        role.setId(1L);
        role.setName("Administrador");

        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("Sucursal Tocache");

        User user = new User();

        user.setId(1L);
        user.setFullName("Darwin G.");
        user.setEmail("darwin@creditos.com");
        user.setPassword("$2a$10$passwordHash");
        user.setRole(role);
        user.setBranch(branch);
        user.setActive(true);

        return user;
    }

}
