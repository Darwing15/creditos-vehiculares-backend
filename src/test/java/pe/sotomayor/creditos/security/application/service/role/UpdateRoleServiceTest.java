package pe.sotomayor.creditos.security.application.service.role;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;
import pe.sotomayor.creditos.security.application.mapper.role.RoleMapper;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateRoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private UpdateRoleService updateRoleService;

    @Test
    void shouldUpdateRoleWhenNameDoesNotExist() {

        /* Arrange */

        Long roleId = 1L;

        UpdateRoleRequest request = updateRequest();

        Role existingRole = createRole(roleId);

        RoleResponse expectedResponse = createResponse(roleId);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));

        when(roleRepository.existsByNameAndIdNot(
                request.name(),
                roleId))
                .thenReturn(false);

        when(roleRepository.save(existingRole)).thenReturn(existingRole);
        when(roleMapper.toResponse(existingRole)).thenReturn(expectedResponse);

        /* Act */

        RoleResponse response = updateRoleService.update(roleId, request);

        /* Assert */

        assertNotNull(response);

        assertEquals(roleId, response.id());

        assertEquals("Administrador", response.name());

        assertEquals(
                "Rol con acceso total al sistema actualizado",
                response.description());

        verify(roleRepository).findById(roleId);

        verify(roleRepository)
                .existsByNameAndIdNot(
                        request.name(),
                        roleId);

        verify(roleMapper)
                .updateEntity(
                        request,
                        existingRole);

        verify(roleRepository).save(existingRole);
        verify(roleMapper).toResponse(existingRole);

        verifyNoMoreInteractions(roleRepository, roleMapper);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenRoleDoesNotExist() {

        /* Arrange */

        Long roleId = 99L;

        UpdateRoleRequest request = updateRequest();

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.empty());

        /* Act & Assert */

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> updateRoleService.update(
                                roleId,
                                request)
                );

        assertEquals(
                "ROLE_NOT_FOUND",
                exception.getErrorCode().name());

        verify(roleRepository).findById(roleId);

        verifyNoMoreInteractions(roleRepository, roleMapper);
    }

    @Test
    void shouldThrowConflictExceptionWhenRoleNameAlreadyExists() {

        /* Arrange */

        Long roleId = 1L;

        UpdateRoleRequest request = updateRequest();

        Role existingRole = createRole(roleId);

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.of(existingRole));

        when(roleRepository.existsByNameAndIdNot(
                request.name(),
                roleId))
                .thenReturn(true);

        /* Act & Assert */

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> updateRoleService.update(
                                roleId,
                                request)
                );

        assertEquals(
                "ROLE_ALREADY_EXISTS",
                exception.getErrorCode().name());

        verify(roleRepository).findById(roleId);

        verify(roleRepository)
                .existsByNameAndIdNot(
                        request.name(),
                        roleId);

        verifyNoMoreInteractions(roleRepository, roleMapper);
    }

    private UpdateRoleRequest updateRequest() {
        return new UpdateRoleRequest(
                "Administrador",
                "Rol con acceso total al sistema actualizado"
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

    private RoleResponse createResponse(Long roleId) {
        return new RoleResponse(
                roleId,
                "Administrador",
                "Rol con acceso total al sistema actualizado"
        );
    }

}
