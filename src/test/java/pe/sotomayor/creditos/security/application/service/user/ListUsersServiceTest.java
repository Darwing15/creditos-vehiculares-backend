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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link ListUsersService}.
 *
 * <p>
 * Verifica el comportamiento del caso de uso encargado de consultar
 * los usuarios registrados dentro del sistema.
 * </p>
 *
 * @author DarwinG
 */
@ExtendWith(MockitoExtension.class)
class ListUsersServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ListUsersService listUsersService;

    @Test
    @DisplayName("Debe listar usuarios existentes correctamente")
    void shouldReturnUsersWhenUsersExist() {

        /* Arrange */

        User admin = createUser(
                1L,
                "Administrador"
        );

        User cajero = createUser(
                2L,
                "Cajero"
        );

        List<User> users = List.of(
                admin,
                cajero
        );

        UserResponse adminResponse =
                createResponse(
                        1L,
                        "Administrador"
                );

        UserResponse cajeroResponse =
                createResponse(
                        2L,
                        "Cajero"
                );

        when(userRepository.findAll())
                .thenReturn(users);

        when(userMapper.toResponse(admin))
                .thenReturn(adminResponse);

        when(userMapper.toResponse(cajero))
                .thenReturn(cajeroResponse);

        /* Act */

        List<UserResponse> result = listUsersService.findAll();

        /* Assert */

        assertNotNull(result);

        assertEquals(
                2,
                result.size()
        );

        assertEquals(
                "Administrador",
                result.get(0).fullName()
        );

        assertEquals(
                "Cajero",
                result.get(1).fullName()
        );

        verify(userRepository).findAll();
        verify(userMapper).toResponse(admin);
        verify(userMapper).toResponse(cajero);

        verifyNoMoreInteractions(
                userRepository,
                userMapper
        );

    }

    @Test
    @DisplayName("Debe retornar una lista vacía cuando no existen usuarios")
    void shouldReturnEmptyListWhenNoUsersExist() {

        /* Arrange */

        when(userRepository.findAll())
                .thenReturn(List.of());

        /* Act */

        List<UserResponse> result = listUsersService.findAll();

        /* Assert */

        assertNotNull(result);

        assertEquals(
                0,
                result.size()
        );

        verify(userRepository).findAll();

        verifyNoMoreInteractions(
                userRepository,
                userMapper
        );

    }

    private User createUser(
            Long id,
            String fullName) {

        User user = new User();

        user.setId(id);

        user.setFullName(fullName);

        user.setEmail(
                fullName.toLowerCase()
                        .replace(" ", ".")
                        + "@test.com"
        );

        user.setActive(true);

        return user;
    }

    private UserResponse createResponse(
            Long id,
            String fullName) {

        return new UserResponse(
                id,
                fullName,
                fullName.toLowerCase()
                        .replace(" ", ".")
                        + "@test.com",
                1L,
                "Administrador",
                1L,
                "Sucursal Principal",
                true
        );

    }

}
