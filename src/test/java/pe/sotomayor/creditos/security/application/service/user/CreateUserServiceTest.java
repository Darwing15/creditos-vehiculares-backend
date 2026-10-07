package pe.sotomayor.creditos.security.application.service.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;
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

import static pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission.USERS_WRITE;
import static pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission.USERS_ASSIGN_ROLE;

@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

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

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private CreateUserService service;

    @Test
    void shouldCreateUserWhenDataIsValid() throws Exception {
        /* Arrange */
        CreateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE, USERS_ASSIGN_ROLE)).thenReturn(createContext());
        when(users.existsByEmail(request.email())).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.of(branch));
        UserResponse expected = createResponse();
        when(mapper.toEntity(request)).thenReturn(user);
        when(encoder.encode(request.password())).thenReturn("new-hash");
        when(users.saveAndFlush(user)).thenReturn(user);
        when(mapper.toResponse(user)).thenReturn(expected);

        /* Act */
        UserResponse result = service.create(request);

        /* Assert */
        assertThat(result).isEqualTo(expected);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        assertThat(user.getActive()).isTrue();
        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(user.getRole()).isSameAs(role);
        assertThat(user.getBranch()).isSameAs(branch);
        verify(gate).begin(USERS_WRITE, USERS_ASSIGN_ROLE);
        verify(users).existsByEmail(request.email());
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verify(mapper).toEntity(request);
        verify(encoder).encode(request.password());
        verify(users).saveAndFlush(user);
        verify(gate).finish();
        verify(mapper).toResponse(user);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles, encoder);
    }

    @Test
    void shouldStopBeforeReadingDataWhenWriteGateDeniesAccess() throws Exception {
        /* Arrange */
        CreateUserRequest request = createRequest();
        when(gate.begin(USERS_WRITE, USERS_ASSIGN_ROLE)).thenThrow(new AccessDeniedException("denied"));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.create(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        verify(gate).begin(USERS_WRITE, USERS_ASSIGN_ROLE);
        verifyNoInteractions(users, mapper, branches, roles, encoder);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles, encoder);
    }

    @Test
    void shouldRejectMutationWhenEmailAlreadyExists() throws Exception {
        /* Arrange */
        CreateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE, USERS_ASSIGN_ROLE)).thenReturn(createContext());
        when(users.existsByEmail(request.email())).thenReturn(true);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.create(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ConflictException.class).extracting("errorCode").isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
        verify(gate).begin(USERS_WRITE, USERS_ASSIGN_ROLE);
        verify(users).existsByEmail(request.email());
        verifyNoInteractions(mapper, encoder);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles, encoder);
    }

    @Test
    void shouldThrowNotFoundWhenRoleDoesNotExist() throws Exception {
        /* Arrange */
        CreateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE, USERS_ASSIGN_ROLE)).thenReturn(createContext());
        when(users.existsByEmail(request.email())).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.create(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).extracting("errorCode").isEqualTo(ErrorCode.ROLE_NOT_FOUND);
        verify(gate).begin(USERS_WRITE, USERS_ASSIGN_ROLE);
        verify(users).existsByEmail(request.email());
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verifyNoInteractions(mapper, encoder);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles, encoder);
    }

    @Test
    void shouldThrowNotFoundWhenBranchDoesNotExist() throws Exception {
        /* Arrange */
        CreateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE, USERS_ASSIGN_ROLE)).thenReturn(createContext());
        when(users.existsByEmail(request.email())).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.create(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BRANCH_NOT_FOUND);
        verify(gate).begin(USERS_WRITE, USERS_ASSIGN_ROLE);
        verify(users).existsByEmail(request.email());
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verifyNoInteractions(mapper, encoder);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles, encoder);
    }

    @Test
    void shouldRejectMutationWhenBranchIsInactive() throws Exception {
        /* Arrange */
        CreateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE, USERS_ASSIGN_ROLE)).thenReturn(createContext());
        when(users.existsByEmail(request.email())).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.of(branch));
        branch.setActive(false);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.create(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo(ErrorCode.BRANCH_INACTIVE);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        verify(gate).begin(USERS_WRITE, USERS_ASSIGN_ROLE);
        verify(users).existsByEmail(request.email());
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verifyNoInteractions(mapper, encoder);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles, encoder);
    }

    @Test
    void shouldRejectMutationWhenTargetHasSuperiorPermissions() throws Exception {
        /* Arrange */
        CreateUserRequest request = createRequest();
        User user = createUser();
        when(gate.begin(USERS_WRITE, USERS_ASSIGN_ROLE)).thenReturn(new WriteContext(99L, Set.of("security:users:write", "security:users:assign-role")));
        when(users.existsByEmail(request.email())).thenReturn(false);
        Role role = createRole();
        when(roles.findByIdAndDeletedFalse(request.roleId())).thenReturn(Optional.of(role));
        Branch branch = createBranch();
        when(branches.findByIdAndDeletedFalse(request.branchId())).thenReturn(Optional.of(branch));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.create(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        verify(gate).begin(USERS_WRITE, USERS_ASSIGN_ROLE);
        verify(users).existsByEmail(request.email());
        verify(roles).findByIdAndDeletedFalse(request.roleId());
        verify(branches).findByIdAndDeletedFalse(request.branchId());
        verifyNoInteractions(mapper, encoder);
        verifyNoMoreInteractions(users, mapper, branches, gate, roles, encoder);
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

    private CreateUserRequest createRequest() {
        return new CreateUserRequest(
                "Ana Perez",
                "ana@test.com",
                "a strong password",
                2L,
                3L
        );
    }

}
