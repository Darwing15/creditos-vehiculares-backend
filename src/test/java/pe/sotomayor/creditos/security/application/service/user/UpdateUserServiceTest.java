package pe.sotomayor.creditos.security.application.service.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import pe.sotomayor.creditos.security.application.dto.user.request.UpdateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.application.mapper.user.UserMapper;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;
import pe.sotomayor.creditos.security.domain.repository.BranchRepository;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;
import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate.WriteContext;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private UserMapper mapper;

    @Mock
    private BranchRepository branches;

    @Mock
    private SecurityWriteGate gate;

    @Mock
    private RoleRepository roles;

    @InjectMocks
    private UpdateUserService service;

    @Test
    void shouldUpdateUserWhenDataIsValid() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.of(branch));
        UserResponse expected = createResponse();
        when(users.saveAndFlush(user)).thenReturn(user);
        when(mapper.toResponse(user)).thenReturn(expected);

        /* Act */
        UserResponse result = service.update(1L, request);

        /* Assert */
        assertThat(result).isEqualTo(expected);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        assertThat(user.getActive()).isTrue();
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(user.getRole()).isSameAs(role);
        assertThat(user.getBranch()).isSameAs(branch);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(users).existsByEmailAndIdNot(request.email(), 1L);
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verify(mapper).updateEntity(request, user);
        verify(users).saveAndFlush(user);
        verify(gate).finish();
        verify(mapper).toResponse(user);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
    }

    @Test
    void shouldStopBeforeReadingDataWhenWriteGateDeniesAccess() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        when(gate.begin(USERS_WRITE)).thenThrow(new AccessDeniedException("denied"));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(1L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        verify(gate).begin(USERS_WRITE);
        verifyNoInteractions(users, mapper, branches, roles);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
    }

    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(1L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
    }

    @Test
    void shouldRejectMutationWhenEmailAlreadyExists() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(true);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(1L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(users).existsByEmailAndIdNot(request.email(), 1L);
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
    }

    @Test
    void shouldThrowNotFoundWhenRoleDoesNotExist() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(1L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ROLE_NOT_FOUND);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(users).existsByEmailAndIdNot(request.email(), 1L);
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
    }

    @Test
    void shouldThrowNotFoundWhenBranchDoesNotExist() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(1L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).extracting("errorCode").isEqualTo(ErrorCode.BRANCH_NOT_FOUND);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(users).existsByEmailAndIdNot(request.email(), 1L);
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
    }

    @Test
    void shouldRejectMutationWhenBranchIsInactive() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.of(branch));
        branch.setActive(false);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(1L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ValidationException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BRANCH_INACTIVE);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(users).existsByEmailAndIdNot(request.email(), 1L);
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
    }

    @Test
    void shouldRejectMutationWhenTargetHasSuperiorPermissions() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(new WriteContext(99L, Set.of("security:users:write", "security:users:assign-role")));
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.of(branch));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(1L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(users).existsByEmailAndIdNot(request.email(), 1L);
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
    }

    @Test
    void shouldRevokeTokensWhenEmailChanges() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.of(branch));
        user.setEmail("old@test.com");
        UserResponse expected = createResponse();
        when(users.saveAndFlush(user)).thenReturn(user);
        when(mapper.toResponse(user)).thenReturn(expected);

        /* Act */
        UserResponse result = service.update(1L, request);

        /* Assert */
        assertThat(result).isEqualTo(expected);
        assertThat(user.getTokenVersion()).isEqualTo(2L);
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(users).existsByEmailAndIdNot(request.email(), 1L);
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verify(mapper).updateEntity(request, user);
        verify(users).saveAndFlush(user);
        verify(gate).finish();
        verify(mapper).toResponse(user);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
    }

    @Test
    void shouldRevokeTokensWhenRoleChanges() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.of(branch));
        user.getRole().setId(4L);
        UserResponse expected = createResponse();
        when(users.saveAndFlush(user)).thenReturn(user);
        when(mapper.toResponse(user)).thenReturn(expected);

        /* Act */
        UserResponse result = service.update(1L, request);

        /* Assert */
        assertThat(result).isEqualTo(expected);
        assertThat(user.getTokenVersion()).isEqualTo(2L);
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(users).existsByEmailAndIdNot(request.email(), 1L);
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verify(mapper).updateEntity(request, user);
        verify(users).saveAndFlush(user);
        verify(gate).finish();
        verify(mapper).toResponse(user);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
    }

    @Test
    void shouldRevokeTokensWhenBranchChanges() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.of(branch));
        user.getBranch().setId(4L);
        when(branches.existsByManagerId(1L)).thenReturn(false);
        UserResponse expected = createResponse();
        when(users.saveAndFlush(user)).thenReturn(user);
        when(mapper.toResponse(user)).thenReturn(expected);

        /* Act */
        UserResponse result = service.update(1L, request);

        /* Assert */
        assertThat(result).isEqualTo(expected);
        assertThat(user.getTokenVersion()).isEqualTo(2L);
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(users).existsByEmailAndIdNot(request.email(), 1L);
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verify(branches).existsByManagerId(1L);
        verify(mapper).updateEntity(request, user);
        verify(users).saveAndFlush(user);
        verify(gate).finish();
        verify(mapper).toResponse(user);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
    }

    @Test
    void shouldRejectUpdateWhenRoleAssignmentPermissionIsMissing() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(new WriteContext(99L, Set.of("security:users:write", "security:users:read")));
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.of(branch));
        role.setId(4L);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(1L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(users).existsByEmailAndIdNot(request.email(), 1L);
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
    }

    @Test
    void shouldRejectUpdateWhenNewRoleHasSuperiorPermissions() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(new WriteContext(99L, Set.of("security:users:write", "security:users:read", "security:users:assign-role")));
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.of(branch));
        role.setId(4L);
        role.getPermissions().add("security:roles:write");

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(1L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(users).existsByEmailAndIdNot(request.email(), 1L);
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
    }

    @Test
    void shouldRejectUpdateWhenBranchManagerWouldMove() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(users.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.of(branch));
        branch.setId(4L);
        when(branches.existsByManagerId(1L)).thenReturn(true);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(1L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(users).existsByEmailAndIdNot(request.email(), 1L);
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verify(branches).existsByManagerId(1L);
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles);
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

    private WriteContext createContext() {
        return new WriteContext(99L, SecurityPermission.authorities());
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

    private UpdateUserRequest createRequest() {
        return new UpdateUserRequest(
                "Ana Perez",
                "ana@test.com",
                2L,
                3L
        );
    }

}
