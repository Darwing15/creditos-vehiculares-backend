package pe.sotomayor.creditos.security.application.service.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.security.application.dto.user.request.UpdateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.application.mapper.user.UserMapper;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.repository.BranchRepository;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link UpdateUserService}.
 *
 * <p>
 * Verifica la actualización administrativa de usuarios existentes.
 * </p>
 *
 * @author DarwinG
 */
@ExtendWith(MockitoExtension.class)
class UpdateUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UpdateUserService updateUserService;

    @Test
    @DisplayName("Debe actualizar un usuario cuando existe y la información es válida")
    void shouldUpdateUserWhenUserExists() {

        /* Arrange */

        Long userId = 1L;

        Long roleId = 2L;

        Long branchId = 3L;

        UpdateUserRequest request = createRequest();

        User user = createUser(userId);

        Role role = createRole(roleId);

        Branch branch = createBranch(branchId);

        UserResponse expectedResponse = createResponse(userId, roleId, branchId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByEmailAndIdNot(
                request.email(),
                userId))
                .thenReturn(false);

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.of(role));

        when(branchRepository.findById(branchId))
                .thenReturn(Optional.of(branch));

        when(userRepository.save(user))
                .thenReturn(user);

        when(userMapper.toResponse(user))
                .thenReturn(expectedResponse);

        /* Act */

        UserResponse response = updateUserService.update(userId, request);

        /* Assert */

        assertNotNull(response);

        assertEquals(
                userId,
                response.id()
        );

        assertEquals(
                "Juan Pérez",
                response.fullName()
        );

        assertEquals(
                "juan@test.com",
                response.email()
        );

        verify(userRepository).findById(userId);

        verify(userRepository)
                .existsByEmailAndIdNot(
                        request.email(),
                        userId);

        verify(roleRepository).findById(roleId);
        verify(branchRepository).findById(branchId);

        verify(userMapper)
                .updateEntity(
                        request,
                        user);

        verify(userRepository)
                .save(user);

        verify(userMapper)
                .toResponse(user);

        verifyNoMoreInteractions(
                userRepository,
                roleRepository,
                branchRepository,
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
                        () -> updateUserService.update(
                                userId,
                                createRequest()
                        )
                );

        assertEquals(
                ErrorCode.USER_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(userRepository)
                .findById(userId);

        verifyNoMoreInteractions(
                userRepository,
                roleRepository,
                branchRepository,
                userMapper
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el correo ya pertenece a otro usuario")
    void shouldThrowConflictExceptionWhenEmailAlreadyExists() {

        /* Arrange */

        Long userId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(createUser(userId)));

        when(userRepository.existsByEmailAndIdNot(
                "juan@test.com",
                userId))
                .thenReturn(true);

        /* Act & Assert */

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> updateUserService.update(
                                userId,
                                createRequest()
                        )
                );

        assertEquals(
                ErrorCode.USER_ALREADY_EXISTS,
                exception.getErrorCode()
        );

        verify(userRepository)
                .findById(userId);

        verify(userRepository)
                .existsByEmailAndIdNot(
                        "juan@test.com",
                        userId
                );

        verifyNoMoreInteractions(
                userRepository,
                roleRepository,
                branchRepository,
                userMapper
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el rol no existe")
    void shouldThrowResourceNotFoundExceptionWhenRoleDoesNotExist() {

        /* Arrange */

        Long userId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(createUser(userId)));

        when(userRepository.existsByEmailAndIdNot(
                "juan@test.com",
                userId))
                .thenReturn(false);

        when(roleRepository.findById(2L))
                .thenReturn(Optional.empty());

        /* Act & Assert */

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> updateUserService.update(
                                userId,
                                createRequest()
                        )
                );

        assertEquals(
                ErrorCode.ROLE_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(userRepository)
                .findById(userId);

        verify(userRepository)
                .existsByEmailAndIdNot(
                        "juan@test.com",
                        userId
                );

        verify(roleRepository)
                .findById(2L);

        verifyNoMoreInteractions(
                userRepository,
                roleRepository,
                branchRepository,
                userMapper
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la sucursal no existe")
    void shouldThrowResourceNotFoundExceptionWhenBranchDoesNotExist() {

        /* Arrange */

        Long userId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(createUser(userId)));

        when(userRepository.existsByEmailAndIdNot(
                "juan@test.com",
                userId))
                .thenReturn(false);

        when(roleRepository.findById(2L))
                .thenReturn(Optional.of(createRole(2L)));

        when(branchRepository.findById(3L))
                .thenReturn(Optional.empty());

        /* Act & Assert */

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> updateUserService.update(
                                userId,
                                createRequest()
                        )
                );

        assertEquals(
                ErrorCode.BRANCH_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(userRepository)
                .findById(userId);

        verify(userRepository)
                .existsByEmailAndIdNot(
                        "juan@test.com",
                        userId
                );

        verify(roleRepository)
                .findById(2L);

        verify(branchRepository)
                .findById(3L);

        verifyNoMoreInteractions(
                userRepository,
                roleRepository,
                branchRepository,
                userMapper
        );
    }

    private UpdateUserRequest createRequest() {
        return new UpdateUserRequest(
                "Juan Pérez",
                "juan@test.com",
                2L,
                3L
        );
    }

    private User createUser(Long id) {

        User user = new User();

        user.setId(id);
        user.setFullName("Usuario anterior");
        user.setEmail("old@test.com");

        return user;
    }

    private Role createRole(Long id) {

        Role role = new Role();

        role.setId(id);
        role.setName("Administrador");

        return role;
    }

    private Branch createBranch(Long id) {

        Branch branch = new Branch();

        branch.setId(id);
        branch.setName("Sede Tocache");

        return branch;
    }

    private UserResponse createResponse(Long userId, Long roleId, Long branchId) {
        return new UserResponse(
                userId,
                "Juan Pérez",
                "juan@test.com",
                roleId,
                "Administrador",
                branchId,
                "Sede Tocache",
                true
        );
    }

}
