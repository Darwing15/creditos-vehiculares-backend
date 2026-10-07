package pe.sotomayor.creditos.security.application.service.role;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;

import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;
import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate.WriteContext;

import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRolePermissionsRequest;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission.ROLES_MANAGE_PERMISSIONS;

@ExtendWith(MockitoExtension.class)
class UpdateRolePermissionsServiceTest {
    @Mock
    private RoleRepository roles;

    @Mock
    private SecurityWriteGate gate;

    @InjectMocks
    private UpdateRolePermissionsService service;

    @Test
    void shouldReplacePermissionsWhenActorCanManageBothSets() throws Exception {
        /* Arrange */
        Role role = createRole();
        UpdateRolePermissionsRequest request = createRequest();
        when(gate.begin(ROLES_MANAGE_PERMISSIONS)).thenReturn(createContext());
        when(roles.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(role));

        /* Act */
        Set<String> result = service.update(2L, request);

        /* Assert */
        assertThat(result).containsExactly("security:roles:read");
        assertThat(role.getPermissions()).containsExactly("security:roles:read");
        assertThat(result).isNotSameAs(role.getPermissions());
        verify(gate).begin(ROLES_MANAGE_PERMISSIONS);
        verify(roles).findByIdAndDeletedFalse(2L);
        verify(roles).saveAndFlush(role);
        verify(gate).finish();
        verifyNoMoreInteractions(roles, gate);
    }

    @Test
    void shouldRemoveAllPermissionsWhenRequestedSetIsEmpty() throws Exception {
        /* Arrange */
        Role role = createRole();
        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest(Set.of());
        when(gate.begin(ROLES_MANAGE_PERMISSIONS)).thenReturn(createContext());
        when(roles.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(role));

        /* Act */
        Set<String> result = service.update(2L, request);

        /* Assert */
        assertThat(result).isEmpty();
        assertThat(role.getPermissions()).isEmpty();
        verify(gate).begin(ROLES_MANAGE_PERMISSIONS);
        verify(roles).findByIdAndDeletedFalse(2L);
        verify(roles).saveAndFlush(role);
        verify(gate).finish();
        verifyNoMoreInteractions(roles, gate);
    }

    @Test
    void shouldRejectPermissionsWhenUnknown() throws Exception {
        /* Arrange */
        Role role = createRole();
        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest(Set.of("super-admin"));
        when(gate.begin(ROLES_MANAGE_PERMISSIONS)).thenReturn(createContext());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(2L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_PERMISSION);
        verify(gate).begin(ROLES_MANAGE_PERMISSIONS);
        verifyNoInteractions(roles);
        verifyNoMoreInteractions(roles, gate);
    }

    @Test
    void shouldRejectPermissionsWhenBlank() throws Exception {
        /* Arrange */
        Role role = createRole();
        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest(Set.of(" "));
        when(gate.begin(ROLES_MANAGE_PERMISSIONS)).thenReturn(createContext());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(2L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_PERMISSION);
        verify(gate).begin(ROLES_MANAGE_PERMISSIONS);
        verifyNoInteractions(roles);
        verifyNoMoreInteractions(roles, gate);
    }

    @Test
    void shouldRejectPermissionsWhenNullSet() throws Exception {
        /* Arrange */
        Role role = createRole();
        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest(null);
        when(gate.begin(ROLES_MANAGE_PERMISSIONS)).thenReturn(createContext());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(2L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_PERMISSION);
        verify(gate).begin(ROLES_MANAGE_PERMISSIONS);
        verifyNoInteractions(roles);
        verifyNoMoreInteractions(roles, gate);
    }

    @Test
    void shouldRejectPermissionsWhenNullElement() throws Exception {
        /* Arrange */
        Role role = createRole();
        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest(new HashSet<>(Arrays.asList((String) null)));
        when(gate.begin(ROLES_MANAGE_PERMISSIONS)).thenReturn(createContext());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(2L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_PERMISSION);
        verify(gate).begin(ROLES_MANAGE_PERMISSIONS);
        verifyNoInteractions(roles);
        verifyNoMoreInteractions(roles, gate);
    }

    @Test
    void shouldRejectChangeWhenExistingPermissionsExceedActor() throws Exception {
        /* Arrange */
        Role role = createRole();
        UpdateRolePermissionsRequest request = createRequest();
        when(gate.begin(ROLES_MANAGE_PERMISSIONS)).thenReturn(new WriteContext(99L, Set.of("security:roles:manage-permissions", "security:roles:read")));
        when(roles.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(role));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(2L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        assertThat(role.getPermissions()).containsExactly("security:users:read");
        verify(gate).begin(ROLES_MANAGE_PERMISSIONS);
        verify(roles).findByIdAndDeletedFalse(2L);
        verifyNoMoreInteractions(roles, gate);
    }

    @Test
    void shouldRejectChangeWhenRequestedPermissionsExceedActor() throws Exception {
        /* Arrange */
        Role role = createRole();
        UpdateRolePermissionsRequest request = createRequest();
        when(gate.begin(ROLES_MANAGE_PERMISSIONS)).thenReturn(new WriteContext(99L, Set.of("security:roles:manage-permissions", "security:users:read")));
        when(roles.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(role));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(2L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        assertThat(role.getPermissions()).containsExactly("security:users:read");
        verify(gate).begin(ROLES_MANAGE_PERMISSIONS);
        verify(roles).findByIdAndDeletedFalse(2L);
        verifyNoMoreInteractions(roles, gate);
    }

    @Test
    void shouldThrowNotFoundWhenRoleDoesNotExist() throws Exception {
        /* Arrange */
        Role role = createRole();
        UpdateRolePermissionsRequest request = createRequest();
        when(gate.begin(ROLES_MANAGE_PERMISSIONS)).thenReturn(createContext());
        when(roles.findByIdAndDeletedFalse(2L)).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(2L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).extracting("errorCode").isEqualTo(ErrorCode.ROLE_NOT_FOUND);
        verify(gate).begin(ROLES_MANAGE_PERMISSIONS);
        verify(roles).findByIdAndDeletedFalse(2L);
        verifyNoMoreInteractions(roles, gate);
    }

    @Test
    void shouldPropagateConflictWhenLastAdminWouldBeRemoved() throws Exception {
        /* Arrange */
        Role role = createRole();
        UpdateRolePermissionsRequest request = createRequest();
        when(gate.begin(ROLES_MANAGE_PERMISSIONS)).thenReturn(createContext());
        when(roles.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(role));
        doThrow(new ConflictException(ErrorCode.LAST_SECURITY_ADMIN_REQUIRED, "last admin")).when(gate).finish();

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(2L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ConflictException.class).extracting("errorCode").isEqualTo(ErrorCode.LAST_SECURITY_ADMIN_REQUIRED);
        verify(gate).begin(ROLES_MANAGE_PERMISSIONS);
        verify(roles).findByIdAndDeletedFalse(2L);
        verify(roles).saveAndFlush(role);
        verify(gate).finish();
        verifyNoMoreInteractions(roles, gate);
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

    private UpdateRolePermissionsRequest createRequest() {
        return new UpdateRolePermissionsRequest(Set.of("security:roles:read"));
    }
}

