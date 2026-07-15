package pe.sotomayor.creditos.security.application.service.role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;
import pe.sotomayor.creditos.security.application.mapper.role.RoleMapper;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link GetRoleService}.
 *
 * <p>
 * Verifica el comportamiento del caso de uso encargado de obtener un rol
 * mediante su identificador.
 * </p>
 *
 * <p>
 * Estas pruebas se ejecutan de forma aislada utilizando Mockito para simular
 * las dependencias externas.
 * </p>
 *
 * @author DarwinG
 */
@ExtendWith(MockitoExtension.class)
class GetRoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private GetRoleService getRoleService;

    @BeforeEach
    void setUp() {

    }

    @Test
    @DisplayName("Debe obtener un rol cuando el identificador existe")
    void shouldReturnRoleWhenIdExists() {

        /* Arrange */
        Long roleId = 1L;

        Role role = createRole(roleId);

        RoleResponse expectedResponse = createResponse(roleId);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(roleMapper.toResponse(role)).thenReturn(expectedResponse);

        /* Act */
        RoleResponse response = getRoleService.getById(roleId);

        /* Assert */
        assertNotNull(response);
        assertEquals(roleId, response.id());
        assertEquals("Administrador", response.name());
        assertEquals("Rol con acceso total al sistema", response.description());

        verify(roleRepository).findById(roleId);
        verify(roleMapper).toResponse(role);

        verifyNoMoreInteractions(roleRepository, roleMapper);
    }

    @Test
    @DisplayName("Debe lanzar una excepción cuando el rol no existe")
    void shouldThrowResourceNotFoundExceptionWhenRoleDoesNotExist() {

        /* Arrange */

        Long roleId = 99L;

        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        /* Act & Assert */

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> getRoleService.getById(roleId));

        assertEquals(ErrorCode.ROLE_NOT_FOUND, exception.getErrorCode());

        verify(roleRepository).findById(roleId);

        verifyNoMoreInteractions(roleRepository, roleMapper);
    }

    private Role createRole(Long id) {

        Role role = new Role();

        role.setId(id);
        role.setName("Administrador");
        role.setDescription("Rol con acceso total al sistema");

        return role;
    }

    private RoleResponse createResponse(Long id) {

        return new RoleResponse(
                id,
                "Administrador",
                "Rol con acceso total al sistema");
    }
}
