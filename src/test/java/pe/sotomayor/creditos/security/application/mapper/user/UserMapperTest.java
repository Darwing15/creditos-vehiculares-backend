package pe.sotomayor.creditos.security.application.mapper.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.request.UpdateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;
import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

/**
 * Pruebas unitarias para {@link UserMapper}.
 *
 * <p>
 * Verifica las conversiones entre los DTO de aplicación y la entidad
 * {@link User}.
 * </p>
 *
 * <p>
 * Estas pruebas validan el contrato del mapper generado por MapStruct.
 * </p>
 *
 * @author DarwinG
 */
class UserMapperTest {

    private final UserMapper userMapper =
            Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("Debe convertir CreateUserRequest en User")
    void shouldMapCreateRequestToEntity() {

        /* Arrange */

        CreateUserRequest request = createRequest();

        /* Act */

        User user = userMapper.toEntity(request);

        /* Assert */

        assertNotNull(user);

        assertEquals(request.fullName(), user.getFullName());
        assertEquals(request.email(), user.getEmail());
    }

    @Test
    @DisplayName("Debe actualizar un usuario existente")
    void shouldUpdateEntity() {

        /* Arrange */

        User user = createUser();

        UpdateUserRequest request = updateRequest();

        /* Act */

        userMapper.updateEntity(request, user);

        /* Assert */

        assertEquals(request.fullName(), user.getFullName());
        assertEquals(request.email(), user.getEmail());
    }

    @Test
    @DisplayName("Debe convertir User en UserResponse")
    void shouldMapEntityToResponse() {

        /* Arrange */

        User user = createUser();

        /* Act */

        UserResponse response = userMapper.toResponse(user);

        /* Assert */

        assertNotNull(response);

        assertEquals(user.getId(), response.id());
        assertEquals(user.getFullName(), response.fullName());
        assertEquals(user.getEmail(), response.email());

        assertEquals(user.getRole().getId(), response.roleId());
        assertEquals(user.getRole().getName(), response.roleName());

        assertEquals(user.getBranch().getId(), response.branchId());
        assertEquals(user.getBranch().getName(), response.branchName());

        assertEquals(user.getActive(), response.active());
    }

    private CreateUserRequest createRequest() {

        return new CreateUserRequest(
                "Raul Sotomayor",
                "admin@creditos.com",
                "Admin123",
                1L,
                1L
        );

    }

    private UpdateUserRequest updateRequest() {

        return new UpdateUserRequest(
                "Raul Sotomayor Gonzales",
                "admin2@creditos.com",
                1L,
                1L
        );

    }

    private User createUser() {

        Role role = new Role();
        role.setId(1L);
        role.setName("Administrador");

        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("Sucursal Principal");

        User user = new User();

        user.setId(1L);
        user.setFullName("Raul Sotomayor");
        user.setEmail("admin@creditos.com");
        user.setRole(role);
        user.setBranch(branch);
        user.setActive(true);

        return user;
    }

}
