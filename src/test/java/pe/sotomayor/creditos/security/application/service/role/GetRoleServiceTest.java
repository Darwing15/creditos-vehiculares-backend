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

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRoleServiceTest {

    @Mock
    private RoleRepository roles;

    @Mock
    private RoleMapper mapper;

    @InjectMocks
    private GetRoleService service;

    @Test
    void shouldReturnRoleDataWhenRoleExists() throws Exception {
        /* Arrange */
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(role));
        RoleResponse expected = createResponse();
        when(mapper.toResponse(role)).thenReturn(expected);

        /* Act */
        RoleResponse result = service.getById(2L);

        /* Assert */
        assertThat(result).isEqualTo(expected);
        verify(roles).findByIdAndDeletedFalse(2L);
        verify(mapper).toResponse(role);
        verifyNoMoreInteractions(roles, mapper);
    }

    @Test
    void shouldThrowNotFoundWhenRoleDoesNotExist() throws Exception {
        /* Arrange */
        when(roles.findByIdAndDeletedFalse(2L)).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.getById(2L));

        /* Assert */
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ROLE_NOT_FOUND);
        verify(roles).findByIdAndDeletedFalse(2L);
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(roles, mapper);
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
        return new RoleResponse(2L, "Operador", "Consulta de usuarios");
    }
}
