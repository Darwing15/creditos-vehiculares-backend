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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListRolesServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private ListRolesService service;

    @Test
    void shouldMapResultsWhenRecordsExist() throws Exception {
        /* Arrange */
        Role entity = createRole();
        RoleResponse expected = createResponse();
        when(roleRepository.findAllByDeletedFalse()).thenReturn(List.of(entity));
        when(roleMapper.toResponse(entity)).thenReturn(expected);

        /* Act */
        List<RoleResponse> result = service.findAll();

        /* Assert */
        assertThat(result).containsExactly(expected);
        verify(roleRepository).findAllByDeletedFalse();
        verify(roleMapper).toResponse(entity);
        verifyNoMoreInteractions(roleRepository, roleMapper);
    }

    @Test
    void shouldReturnEmptyListWhenNoRecordsExist() throws Exception {
        /* Arrange */
        when(roleRepository.findAllByDeletedFalse()).thenReturn(List.of());

        /* Act */
        List<RoleResponse> result = service.findAll();

        /* Assert */
        assertThat(result).isEmpty();
        verify(roleRepository).findAllByDeletedFalse();
        verifyNoInteractions(roleMapper);
        verifyNoMoreInteractions(roleRepository, roleMapper);
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
