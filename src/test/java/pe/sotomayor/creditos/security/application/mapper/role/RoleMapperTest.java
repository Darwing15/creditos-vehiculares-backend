package pe.sotomayor.creditos.security.application.mapper.role;

import org.junit.jupiter.api.Test;

import org.mapstruct.factory.Mappers;

import pe.sotomayor.creditos.security.application.dto.role.request.CreateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRoleRequest;

import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

import pe.sotomayor.creditos.security.domain.model.entity.Role;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoleMapperTest {

    private final RoleMapper mapper =
            Mappers.getMapper(RoleMapper.class);

    @Test
    void shouldMapPublicFieldsWhenCreatingEntity() throws Exception {
        /* Arrange */
        CreateRoleRequest request = createRequest();

        /* Act */
        Role result = mapper.toEntity(request);

        /* Assert */
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        assertThat(result.getName()).isEqualTo(request.name());
        assertThat(result.getDescription()).isEqualTo(request.description());
        assertThat(result.getPermissions()).isEmpty();
        assertThat(result.getRowVersion()).isNull();
    }

    @Test
    void shouldPreserveProtectedFieldsWhenUpdatingEntity() throws Exception {
        /* Arrange */
        Role entity = createRole();
        entity.setRowVersion(7L);
        UpdateRoleRequest request = createUpdateRequest();

        /* Act */
        mapper.updateEntity(request, entity);

        /* Assert */
        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getRowVersion()).isEqualTo(7L);
        assertThat(entity.getName()).isEqualTo(request.name());
        assertThat(entity.getDescription()).isEqualTo(request.description());
        assertThat(entity.getPermissions()).containsExactly("security:users:read");
    }

    @Test
    void shouldMapResponseWhenEntityHasRelations() throws Exception {
        /* Arrange */
        Role entity = createRole();

        /* Act */
        RoleResponse result = mapper.toResponse(entity);

        /* Assert */
        assertThat(result).isEqualTo(createResponse());
    }

    private Role createRole() {
        return Role.builder()
                .id(2L)
                .name("Operador")
                .description("Consulta de usuarios")
                .permissions(new HashSet<>(Set.of("security:users:read")))
                .deleted(false)
                .build();
    }

    private RoleResponse createResponse() {
        return new RoleResponse(
                2L,
                "Operador",
                "Consulta de usuarios"
        );
    }

    private CreateRoleRequest createRequest() {
        return new CreateRoleRequest(
                "Operador",
                "Consulta de usuarios"
        );
    }

    private UpdateRoleRequest createUpdateRequest() {
        return new UpdateRoleRequest(
                "Consulta",
                "Descripcion actualizada"
        );
    }

}
