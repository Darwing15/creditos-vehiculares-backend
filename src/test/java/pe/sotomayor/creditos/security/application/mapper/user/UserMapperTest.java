package pe.sotomayor.creditos.security.application.mapper.user;

import org.junit.jupiter.api.Test;

import org.mapstruct.factory.Mappers;

import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.request.UpdateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper =
            Mappers.getMapper(UserMapper.class);

    @Test
    void shouldMapPublicFieldsWhenCreatingEntity() throws Exception {
        /* Arrange */
        CreateUserRequest request = createRequest();

        /* Act */
        User result = mapper.toEntity(request);

        /* Assert */
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        assertThat(result.getFullName()).isEqualTo(request.fullName());
        assertThat(result.getEmail()).isEqualTo(request.email());
        assertThat(result.getPassword()).isNull();
        assertThat(result.getRole()).isNull();
        assertThat(result.getBranch()).isNull();
    }

    @Test
    void shouldPreserveProtectedFieldsWhenUpdatingEntity() throws Exception {
        /* Arrange */
        User entity = createUser();
        entity.setRowVersion(7L);
        UpdateUserRequest request = createUpdateRequest();

        /* Act */
        mapper.updateEntity(request, entity);

        /* Assert */
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getRowVersion()).isEqualTo(7L);
        assertThat(entity.getFullName()).isEqualTo(request.fullName());
        assertThat(entity.getEmail()).isEqualTo(request.email());
        assertThat(entity.getPassword()).isEqualTo("encoded-password");
        assertThat(entity.getTokenVersion()).isEqualTo(1L);
        assertThat(entity.getRole().getId()).isEqualTo(2L);
        assertThat(entity.getBranch().getId()).isEqualTo(3L);
        assertThat(entity.getActive()).isTrue();
    }

    @Test
    void shouldMapResponseWhenEntityHasRelations() throws Exception {
        /* Arrange */
        User entity = createUser();

        /* Act */
        UserResponse result = mapper.toResponse(entity);

        /* Assert */
        assertThat(result).isEqualTo(createResponse());
    }

    private User createUser() {
        return User.builder()
                .id(1L)
                .fullName("Ana Perez")
                .email("ana@test.com")
                .password("encoded-password")
                .active(true)
                .deleted(false)
                .tokenVersion(1L)
                .role(createRole())
                .branch(createBranch())
                .build();
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

    private Branch createBranch() {
        return Branch.builder()
                .id(3L)
                .code("LIM")
                .name("Lima")
                .address("Av. Peru 123")
                .email("lima@test.com")
                .phone("999999999")
                .active(true)
                .deleted(false)
                .build();
    }

    private UserResponse createResponse() {
        return new UserResponse(
                1L,
                "Ana Perez",
                "ana@test.com",
                2L,
                "Operador",
                3L,
                "Lima",
                true
        );
    }

    private CreateUserRequest createRequest() {
        return new CreateUserRequest(
                "Ana Perez",
                "ana@test.com",
                "a strong password",
                2L,
                3L
        );
    }

    private UpdateUserRequest createUpdateRequest() {
        return new UpdateUserRequest(
                "Ana Actualizada",
                "updated@test.com",
                4L,
                5L
        );
    }

}
