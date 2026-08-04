package pe.sotomayor.creditos.security.application.service.role;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;
import pe.sotomayor.creditos.security.application.mapper.role.RoleMapper;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
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

@ExtendWith(MockitoExtension.class)
class DeactivateRoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private DeactivateRoleService deactivateRoleService;

    @Test
    void shouldDeactivateRoleWhenRoleExistsAndIsNotInUse() {

        /* Arrange */

        Long roleId = 1L;

        Role role = createRole(roleId);

        RoleResponse expectedResponse = createResponse(roleId);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        when(userRepository.existsByRoleId(roleId)).thenReturn(false);

        when(roleRepository.save(role)).thenReturn(role);

        when(roleMapper.toResponse(role)).thenReturn(expectedResponse);

        /* Act */

        RoleResponse response =
                deactivateRoleService.deactivate(roleId);

        /* Assert */

        assertEquals(true, role.getDeleted());
        assertNotNull(role.getDeletedAt());
        assertNotNull(response);
        assertEquals(roleId, response.id());

        assertEquals(
                "Administrador",
                response.name());

        assertEquals(
                "Rol con acceso total al sistema",
                response.description());

        verify(roleRepository).findById(roleId);
        verify(userRepository).existsByRoleId(roleId);
        verify(roleRepository).save(role);
        verify(roleMapper).toResponse(role);

        verifyNoMoreInteractions(
                roleRepository,
                userRepository,
                roleMapper);
    }

    @Test
    void shouldThrowConflictExceptionWhenRoleHasAssignedUsers() {

        /* Arrange */

        Long roleId = 1L;

        Role role = createRole(roleId);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userRepository.existsByRoleId(roleId)).thenReturn(true);

        /* Act & Assert */

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> deactivateRoleService.deactivate(roleId)
        );

        assertEquals(
                ErrorCode.ROLE_IN_USE,
                exception.getErrorCode()
        );

        verify(roleRepository).findById(roleId);
        verify(userRepository).existsByRoleId(roleId);

        verifyNoMoreInteractions(
                roleRepository,
                userRepository,
                roleMapper
        );
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenRoleDoesNotExist() {

        /* Arrange */

        Long roleId = 1L;

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.empty());

        /* Act & Assert */

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> deactivateRoleService.deactivate(roleId)
        );

        assertEquals(
                ErrorCode.ROLE_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(roleRepository).findById(roleId);

        verifyNoMoreInteractions(
                roleRepository,
                userRepository,
                roleMapper
        );
    }

    private Role createRole(Long id) {

        Role role = new Role();

        role.setId(id);

        role.setName("Administrador");

        role.setDescription(
                "Rol con acceso total al sistema");

        return role;
    }

    private RoleResponse createResponse(Long id) {

        return new RoleResponse(
                id,
                "Administrador",
                "Rol con acceso total al sistema");
    }

}
