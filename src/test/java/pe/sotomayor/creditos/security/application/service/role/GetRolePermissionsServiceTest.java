package pe.sotomayor.creditos.security.application.service.role;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRolePermissionsServiceTest {

    @Mock
    private RoleRepository roles;

    @InjectMocks
    private GetRolePermissionsService service;

    @Test
    void shouldReturnRoleDataWhenRoleExists() throws Exception {
        /* Arrange */
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(role));

        /* Act */
        Set<String> result = service.getById(2L);

        /* Assert */
        assertThat(result).containsExactly("security:users:read");
        assertThat(result).isNotSameAs(role.getPermissions());
        verify(roles).findByIdAndDeletedFalse(2L);
        verifyNoMoreInteractions(roles);
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
        verifyNoMoreInteractions(roles);
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
}
