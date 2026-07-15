package pe.sotomayor.creditos.security.application.service.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;
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
 * Pruebas unitarias para {@link CreateUserService}.
 *
 * <p>
 * Verifica el comportamiento del caso de uso encargado de registrar usuarios
 * dentro del módulo de seguridad.
 * </p>
 *
 * @author DarwinG
 */
@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CreateUserService createUserService;

    @Test
    @DisplayName("Debe crear usuario cuando los datos son válidos")
    void shouldCreateUserWhenDataIsValid() {

        /* Arrange */

        CreateUserRequest request = createRequest();

        Role role = new Role();
        role.setId(1L);
        role.setName("Administrador");

        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("Sucursal Principal");

        User user = new User();

        User savedUser = new User();

        UserResponse expectedResponse = createResponse();

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);

        when(roleRepository.findById(request.roleId()))
                .thenReturn(Optional.of(role));

        when(branchRepository.findById(request.branchId()))
                .thenReturn(Optional.of(branch));

        when(userMapper.toEntity(request))
                .thenReturn(user);

        when(passwordEncoder.encode(request.password()))
                .thenReturn("encoded-password");

        when(userRepository.save(user))
                .thenReturn(savedUser);

        when(userMapper.toResponse(savedUser))
                .thenReturn(expectedResponse);

        /* Act */

        UserResponse response = createUserService.create(request);

        /* Assert */

        assertNotNull(response);

        assertEquals(
                "Raul Sotomayor",
                response.fullName());

        assertEquals(
                "darwin@test.com",
                response.email());

        verify(userRepository)
                .existsByEmail(request.email());

        verify(roleRepository)
                .findById(request.roleId());

        verify(branchRepository)
                .findById(request.branchId());

        verify(userMapper)
                .toEntity(request);

        verify(passwordEncoder)
                .encode(request.password());

        verify(userRepository)
                .save(user);

        verify(userMapper)
                .toResponse(savedUser);

        verifyNoMoreInteractions(
                userRepository,
                roleRepository,
                branchRepository,
                userMapper,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el correo del usuario ya existe")
    void shouldThrowConflictExceptionWhenEmailAlreadyExists() {

        /* Arrange */

        CreateUserRequest request = createRequest();

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(true);

        /* Act & Assert */

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> createUserService.create(request)
                );

        assertEquals(
                ErrorCode.USER_ALREADY_EXISTS,
                exception.getErrorCode()
        );

        verify(userRepository)
                .existsByEmail(request.email());

        verifyNoMoreInteractions(
                userRepository,
                roleRepository,
                branchRepository,
                userMapper,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el rol no existe")
    void shouldThrowResourceNotFoundExceptionWhenRoleDoesNotExist() {

        /* Arrange */

        CreateUserRequest request = createRequest();

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);

        when(roleRepository.findById(request.roleId()))
                .thenReturn(Optional.empty());

        /* Act & Assert */

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> createUserService.create(request)
                );

        assertEquals(
                ErrorCode.ROLE_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(userRepository)
                .existsByEmail(request.email());

        verify(roleRepository)
                .findById(request.roleId());

        verifyNoMoreInteractions(
                userRepository,
                roleRepository,
                branchRepository,
                userMapper,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la sucursal no existe")
    void shouldThrowResourceNotFoundExceptionWhenBranchDoesNotExist() {

        /* Arrange */

        CreateUserRequest request = createRequest();

        Role role = new Role();

        role.setId(1L);

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);

        when(roleRepository.findById(request.roleId()))
                .thenReturn(Optional.of(role));

        when(branchRepository.findById(request.branchId()))
                .thenReturn(Optional.empty());

        /* Act & Assert */

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> createUserService.create(request)
                );

        assertEquals(
                ErrorCode.BRANCH_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(userRepository)
                .existsByEmail(request.email());

        verify(roleRepository)
                .findById(request.roleId());

        verify(branchRepository)
                .findById(request.branchId());

        verifyNoMoreInteractions(
                userRepository,
                roleRepository,
                branchRepository,
                userMapper,
                passwordEncoder
        );
    }

    private CreateUserRequest createRequest() {
        return new CreateUserRequest(
                "Raul Sotomayor",
                "darwin@test.com",
                "password123",
                1L,
                1L
        );
    }

    private UserResponse createResponse() {
        return new UserResponse(
                1L,
                "Raul Sotomayor",
                "darwin@test.com",
                1L,
                "Administrador",
                1L,
                "Sucursal Principal",
                true
        );
    }

}
