package pe.sotomayor.creditos.security.application.mapper.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import pe.sotomayor.creditos.security.application.dto.role.request.CreateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;
import pe.sotomayor.creditos.security.domain.model.entity.Role;

/**
 * Pruebas unitarias para {@link RoleMapper}.
 *
 * <p>
 * Verifica las conversiones entre los DTO de aplicación y la entidad de
 * dominio {@link Role}.
 * </p>
 *
 * <p>
 * Estas pruebas validan el contrato del mapper generado por MapStruct.
 * </p>
 *
 * @author DarwinG
 */
class RoleMapperTest {

    private final RoleMapper roleMapper =
            Mappers.getMapper(RoleMapper.class);

    @Test
    @DisplayName("Debe convertir CreateRoleRequest en Role")
    void shouldMapRequestToEntity() {
        /* Arrange */
        CreateRoleRequest request = createRequest();

        /* Act */
        Role role = roleMapper.toEntity(request);

        /* Assert */
        assertNotNull(role);
        assertEquals(request.name(), role.getName());
        assertEquals(request.description(), role.getDescription());
    }

    @Test
    @DisplayName("Debe actualizar un rol existente")
    void shouldUpdateEntity() {

        /* Arrange */
        Role role = createRole();

        UpdateRoleRequest request = updateRequest();

        /* Act */

        roleMapper.updateEntity(request, role);

        /* Assert */

        assertEquals(request.name(), role.getName());
        assertEquals(request.description(), role.getDescription());

    }

    @Test
    @DisplayName("Debe convertir Role en RoleResponse")
    void shouldMapEntityToResponse() {
        /* Arrange */
        Role role = createRole();

        /* Act */
        RoleResponse response = roleMapper.toResponse(role);

        /* Assert */
        assertNotNull(response);
        assertEquals(role.getId(), response.id());
        assertEquals(role.getName(), response.name());
        assertEquals(role.getDescription(), response.description());
    }

    private CreateRoleRequest createRequest() {
        return new CreateRoleRequest(
                "Administrador",
                "Rol con acceso completo al sistema"
        );
    }

    private Role createRole() {

        Role role = new Role();

        role.setId(1L);
        role.setName("Administrador");
        role.setDescription("Rol con acceso completo al sistema");

        return role;
    }

    private UpdateRoleRequest updateRequest() {
        return new UpdateRoleRequest(
                "Administrador",
                "Rol con acceso completo al sistema actualizado"
        );
    }

}
