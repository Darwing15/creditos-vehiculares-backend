package pe.sotomayor.creditos.security.application.service.role;

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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link ListRolesService}.
 *
 * <p>
 * Verifica el comportamiento del caso de uso encargado de consultar
 * los roles registrados dentro del sistema.
 * </p>
 *
 * @author DarwinG
 */
@ExtendWith(MockitoExtension.class)
class ListRolesServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private ListRolesService listRolesService;

    @Test
    @DisplayName("Debe listar roles existentes correctamente")
    void shouldReturnRolesWhenRolesExist() {

        /* Arrange */

        Role admin = createRole(
                1L,
                "Administrador");

        Role cajero = createRole(
                2L,
                "Cajero");

        List<Role> roles = List.of(
                admin,
                cajero);

        List<RoleResponse> responses = List.of(
                createResponse(1L, "Administrador"),
                createResponse(2L, "Cajero"));

        when(roleRepository.findAll()).thenReturn(roles);
        when(roleMapper.toResponse(admin)).thenReturn(responses.get(0));
        when(roleMapper.toResponse(cajero)).thenReturn(responses.get(1));

        /* Act */

        List<RoleResponse> result = listRolesService.findAll();

        /* Assert */

        assertNotNull(result);
        assertEquals(
                2,
                result.size());

        assertEquals(
                "Administrador",
                result.get(0).name());

        assertEquals(
                "Cajero",
                result.get(1).name());

        verify(roleRepository).findAll();
        verify(roleMapper).toResponse(admin);
        verify(roleMapper).toResponse(cajero);

        verifyNoMoreInteractions(
                roleRepository,
                roleMapper);
    }

    @Test
    @DisplayName("Debe retornar una lista vacía cuando no existen roles registrados")
    void shouldReturnEmptyListWhenNoRolesExist() {

        /* Arrange */

        when(roleRepository.findAll()).thenReturn(List.of());

        /* Act */

        List<RoleResponse> result = listRolesService.findAll();

        /* Assert */

        assertNotNull(result);
        assertEquals(
                0,
                result.size());

        verify(roleRepository).findAll();

        verifyNoMoreInteractions(
                roleRepository,
                roleMapper);
    }

    private Role createRole(
            Long id,
            String name) {

        Role role = new Role();

        role.setId(id);
        role.setName(name);
        role.setDescription(
                "Descripción del rol");

        return role;
    }

    private RoleResponse createResponse(
            Long id,
            String name) {

        return new RoleResponse(
                id,
                name,
                "Descripción del rol");
    }

}
