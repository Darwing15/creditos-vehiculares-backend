package pe.sotomayor.creditos.security.application.service.role;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import pe.sotomayor.creditos.security.application.dto.role.request.CreateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

import pe.sotomayor.creditos.security.application.mapper.role.RoleMapper;

import pe.sotomayor.creditos.security.domain.model.entity.Role;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;

import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;
import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate.WriteContext;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission.ROLES_WRITE;

@ExtendWith(MockitoExtension.class)
class CreateRoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private SecurityWriteGate gate;

    @InjectMocks
    private CreateRoleService service;

    @Test
    void shouldCreateRoleWhenDataIsValid() throws Exception {
        /* Arrange */
        CreateRoleRequest request = createRequest();
        Role entity = createRole();
        when(gate.begin(ROLES_WRITE)).thenReturn(createContext());
        when(roleRepository.existsByName(request.name())).thenReturn(false);
        RoleResponse expected = createResponse();
        when(roleMapper.toEntity(request)).thenReturn(entity);
        when(roleRepository.saveAndFlush(entity)).thenReturn(entity);
        when(roleMapper.toResponse(entity)).thenReturn(expected);

        /* Act */
        RoleResponse result = service.create(request);

        /* Assert */
        assertThat(result).isEqualTo(expected);

        verify(gate).begin(ROLES_WRITE);
        verify(roleRepository).existsByName(request.name());
        verify(roleMapper).toEntity(request);
        verify(roleRepository).saveAndFlush(entity);
        verify(gate).finish();
        verify(roleMapper).toResponse(entity);
        verifyNoMoreInteractions(roleRepository, roleMapper, gate);
    }

    @Test
    void shouldStopBeforeReadingDataWhenWriteGateDeniesAccess() throws Exception {
        /* Arrange */
        CreateRoleRequest request = createRequest();
        when(gate.begin(ROLES_WRITE)).thenThrow(new AccessDeniedException("denied"));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.create(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        verify(gate).begin(ROLES_WRITE);
        verifyNoInteractions(roleRepository, roleMapper);
        verifyNoMoreInteractions(roleRepository, roleMapper, gate);
    }

    @Test
    void shouldRejectDuplicateWhenNameAlreadyExists() throws Exception {
        /* Arrange */
        CreateRoleRequest request = createRequest();
        Role entity = createRole();
        when(gate.begin(ROLES_WRITE)).thenReturn(createContext());
        when(roleRepository.existsByName(request.name())).thenReturn(true);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.create(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ConflictException.class).extracting("errorCode").isEqualTo(ErrorCode.ROLE_ALREADY_EXISTS);
        verify(gate).begin(ROLES_WRITE);
        verify(roleRepository).existsByName(request.name());
        verifyNoInteractions(roleMapper);
        verifyNoMoreInteractions(roleRepository, roleMapper, gate);
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

    private WriteContext createContext() {
        return new WriteContext(99L, SecurityPermission.authorities());
    }

    private RoleResponse createResponse() {
        return new RoleResponse(
                2L,
                "Operador",
                "Consulta de usuarios"
        );
    }
    private CreateRoleRequest createRequest() {
        return new CreateRoleRequest("Analista", "Consulta");
    }
}
