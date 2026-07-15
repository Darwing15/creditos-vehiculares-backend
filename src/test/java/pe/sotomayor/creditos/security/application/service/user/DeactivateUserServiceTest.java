package pe.sotomayor.creditos.security.application.service.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;
import pe.sotomayor.creditos.security.application.mapper.user.UserMapper;

import pe.sotomayor.creditos.security.domain.model.entity.User;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link DeactivateUserService}.
 *
 * <p>
 * Verifica la desactivación funcional de usuarios.
 * </p>
 *
 * @author DarwinG
 */
@ExtendWith(MockitoExtension.class)
class DeactivateUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private DeactivateUserService deactivateUserService;

    @Test
    @DisplayName("Debe desactivar un usuario cuando existe y está activo")
    void shouldDeactivateUserWhenUserExistsAndIsActive() {

        /* Arrange */

        Long userId = 1L;

        User user = createUser(userId);

        UserResponse expectedResponse = createResponse(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user))
                .thenReturn(user);

        when(userMapper.toResponse(user))
                .thenReturn(expectedResponse);

        /* Act */

        UserResponse response = deactivateUserService.deactivate(userId);

        /* Assert */

        assertNotNull(response);

        assertEquals(
                userId,
                response.id()
        );

        assertFalse(user.getActive());
        assertFalse(response.active());

        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);

        verifyNoMoreInteractions(
                userRepository,
                userMapper
        );

    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el usuario no existe")
    void shouldThrowResourceNotFoundExceptionWhenUserDoesNotExist() {

        /* Arrange */

        Long userId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        /* Act & Assert */

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                deactivateUserService.deactivate(userId)
                );

        assertEquals(
                ErrorCode.USER_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(userRepository)
                .findById(userId);

        verifyNoMoreInteractions(
                userRepository,
                userMapper
        );

    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el usuario ya está inactivo")
    void shouldThrowConflictExceptionWhenUserIsAlreadyInactive() {

        /* Arrange */

        Long userId = 1L;

        User user = createUser(userId);

        user.setActive(false);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        /* Act & Assert */

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                deactivateUserService.deactivate(userId)
                );

        assertEquals(
                ErrorCode.USER_INACTIVE,
                exception.getErrorCode()
        );

        verify(userRepository)
                .findById(userId);

        verifyNoMoreInteractions(
                userRepository,
                userMapper
        );

    }

    private User createUser(Long id) {

        User user = new User();

        user.setId(id);

        user.setFullName("Juan Pérez");

        user.setEmail("juan@test.com");

        user.setActive(true);

        return user;
    }

    private UserResponse createResponse(Long userId) {
        return new UserResponse(
                userId,
                "Juan Pérez",
                "juan@test.com",
                1L,
                "Administrador",
                1L,
                "Sede Tocache",
                false
        );
    }

}