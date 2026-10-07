package pe.sotomayor.creditos.security.application.service.role;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;
import pe.sotomayor.creditos.security.application.mapper.role.RoleMapper;

import pe.sotomayor.creditos.security.domain.model.entity.Role;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;
import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate.WriteContext;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
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

import static pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission.ROLES_WRITE;

@ExtendWith(MockitoExtension.class)
class DeactivateRoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private SecurityWriteGate gate;

    @Mock
    private UserRepository users;

    @InjectMocks
    private DeactivateRoleService service;

    @Test
    void shouldDeactivateRoleWhenDataIsValid() throws Exception {
        /* Arrange */
        Role entity = createRole();
        when(gate.begin(ROLES_WRITE)).thenReturn(createContext());
        when(roleRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(entity));
        when(users.existsByRoleId(2L)).thenReturn(false);
        RoleResponse expected = createResponse();
        when(roleRepository.saveAndFlush(entity)).thenReturn(entity);
        when(roleMapper.toResponse(entity)).thenReturn(expected);

        /* Act */
        RoleResponse result = service.deactivate(2L);

        /* Assert */
        assertThat(result).isEqualTo(expected);
        assertThat(entity.getDeleted()).isTrue();
        assertThat(entity.getDeletedAt()).isNotNull();
        verify(gate).begin(ROLES_WRITE);
        verify(roleRepository).findByIdAndDeletedFalse(2L);
        verify(users).existsByRoleId(2L);
        verify(roleRepository).saveAndFlush(entity);
        verify(gate).finish();
        verify(roleMapper).toResponse(entity);
        verifyNoMoreInteractions(roleRepository, roleMapper, gate, users);
    }

    @Test
    void shouldStopBeforeReadingDataWhenWriteGateDeniesAccess() throws Exception {
        /* Arrange */
        when(gate.begin(ROLES_WRITE)).thenThrow(new AccessDeniedException("denied"));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.deactivate(2L));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        verify(gate).begin(ROLES_WRITE);
        verifyNoInteractions(roleRepository, roleMapper, users);
        verifyNoMoreInteractions(roleRepository, roleMapper, gate, users);
    }

    @Test
    void shouldThrowNotFoundWhenRoleDoesNotExist() throws Exception {
        /* Arrange */
        when(gate.begin(ROLES_WRITE)).thenReturn(createContext());
        when(roleRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.deactivate(2L));

        /* Assert */
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).extracting("errorCode").isEqualTo(ErrorCode.ROLE_NOT_FOUND);
        verify(gate).begin(ROLES_WRITE);
        verify(roleRepository).findByIdAndDeletedFalse(2L);
        verifyNoInteractions(roleMapper, users);
        verifyNoMoreInteractions(roleRepository, roleMapper, gate, users);
    }

    @Test
    void shouldRejectDeactivationWhenUsersRemainAssigned() throws Exception {
        /* Arrange */
        Role entity = createRole();
        when(gate.begin(ROLES_WRITE)).thenReturn(createContext());
        when(roleRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(entity));
        when(users.existsByRoleId(2L)).thenReturn(true);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.deactivate(2L));

        /* Assert */
        assertThat(thrown).isInstanceOf(ConflictException.class).extracting("errorCode").isEqualTo(ErrorCode.ROLE_IN_USE);
        verify(gate).begin(ROLES_WRITE);
        verify(roleRepository).findByIdAndDeletedFalse(2L);
        verify(users).existsByRoleId(2L);
        verifyNoInteractions(roleMapper);
        verifyNoMoreInteractions(roleRepository, roleMapper, gate, users);
    }

    @Test
    void shouldRejectMutationWhenTargetHasSuperiorPermissions() throws Exception {
        /* Arrange */
        Role entity = createRole();
        when(gate.begin(ROLES_WRITE)).thenReturn(new WriteContext(99L, Set.of("security:roles:write")));
        when(roleRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(entity));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.deactivate(2L));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        verify(gate).begin(ROLES_WRITE);
        verify(roleRepository).findByIdAndDeletedFalse(2L);
        verifyNoInteractions(roleMapper, users);
        verifyNoMoreInteractions(roleRepository, roleMapper, gate, users);
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

}
