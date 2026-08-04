package pe.sotomayor.creditos.security.application.service.role;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.security.application.dto.role.request.CreateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

import pe.sotomayor.creditos.security.application.mapper.role.RoleMapper;

import pe.sotomayor.creditos.security.domain.model.entity.Role;

import pe.sotomayor.creditos.security.domain.repository.RoleRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateRoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private CreateRoleService createRoleService;

    @Test
    void shouldCreateRoleWhenNameDoesNotExist() {

        /* Arrange */
        CreateRoleRequest request = createRequest();

        Role roleToSave = createRoleToSave();

        Role savedRole = createSavedRole();

        RoleResponse response = createResponse();

        when(roleMapper.toEntity(request)).thenReturn(roleToSave);

        when(roleRepository.save(roleToSave)).thenReturn(savedRole);

        when(roleMapper.toResponse(savedRole)).thenReturn(response);

        /* Act */
        RoleResponse result = createRoleService.create(request);

        /* Assert */
        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Administrador", result.name());
        assertEquals("Rol con acceso completo al sistema", result.description());

        verify(roleMapper).toEntity(request);
        verify(roleRepository).save(roleToSave);
        verify(roleMapper).toResponse(savedRole);
    }

    @Test
    void shouldThrowConflictExceptionWhenRoleNameAlreadyExists() {

        /* Arrange */
        CreateRoleRequest request = createRequest();

        when(roleRepository.existsByName("Administrador"))
                .thenReturn(true);

        /* Act */
        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () -> createRoleService.create(request));

        /* Assert */
        assertEquals(
                ErrorCode.ROLE_ALREADY_EXISTS,
                exception.getErrorCode());

        verify(roleRepository).existsByName("Administrador");
        verify(roleRepository, never()).save(any());
        verify(roleMapper, never()).toEntity(any());
        verify(roleMapper, never()).toResponse(any());
    }

    private CreateRoleRequest createRequest() {
        return new CreateRoleRequest(
                "Administrador",
                "Rol con acceso completo al sistema"
        );
    }

    private Role createRoleToSave() {

        Role role = new Role();

        role.setName("Administrador");
        role.setDescription("Rol con acceso completo al sistema");

        return role;
    }

    private Role createSavedRole() {

        Role role = new Role();

        role.setId(1L);
        role.setName("Administrador");
        role.setDescription("Rol con acceso completo al sistema");

        return role;
    }

    private RoleResponse createResponse() {
        return new RoleResponse(
                1L,
                "Administrador",
                "Rol con acceso completo al sistema");
    }
}
