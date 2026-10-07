package pe.sotomayor.creditos.security.application.service.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;
import pe.sotomayor.creditos.security.application.mapper.user.UserMapper;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;

import pe.sotomayor.creditos.security.domain.repository.BranchRepository;
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

import static pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission.USERS_WRITE;

@ExtendWith(MockitoExtension.class)
class DeactivateUserServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private UserMapper mapper;

    @Mock
    private BranchRepository branches;

    @Mock
    private SecurityWriteGate gate;

    @InjectMocks
    private DeactivateUserService service;

    @Test
    void shouldDeactivateUserWhenDataIsValid() throws Exception {
        /* Arrange */
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(branches.existsByManagerId(1L)).thenReturn(false);
        UserResponse expected = createResponse();
        when(users.saveAndFlush(user)).thenReturn(user);
        when(mapper.toResponse(user)).thenReturn(expected);

        /* Act */
        UserResponse result = service.deactivate(1L);

        /* Assert */
        assertThat(result).isEqualTo(expected);
        assertThat(user.getTokenVersion()).isEqualTo(2L);
        assertThat(user.getActive()).isFalse();
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(branches).existsByManagerId(1L);
        verify(users).saveAndFlush(user);
        verify(gate).finish();
        verify(mapper).toResponse(user);
        verifyNoMoreInteractions(users, mapper, branches, gate);
    }

    @Test
    void shouldStopBeforeReadingDataWhenWriteGateDeniesAccess() throws Exception {
        /* Arrange */
        when(gate.begin(USERS_WRITE)).thenThrow(new AccessDeniedException("denied"));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.deactivate(1L));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        verify(gate).begin(USERS_WRITE);
        verifyNoInteractions(users, mapper, branches);
        verifyNoMoreInteractions(users, mapper, branches, gate);
    }

    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() throws Exception {
        /* Arrange */
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.deactivate(1L));

        /* Assert */
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(users, mapper, branches, gate);
    }

    @Test
    void shouldRejectDeactivationWhenUserIsBranchManager() throws Exception {
        /* Arrange */
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(branches.existsByManagerId(1L)).thenReturn(true);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.deactivate(1L));

        /* Assert */
        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(branches).existsByManagerId(1L);
        verifyNoInteractions(mapper);
        verifyNoMoreInteractions(users, mapper, branches, gate);
    }

    @Test
    void shouldRejectDeactivationWhenTargetIsSelf() throws Exception {
        /* Arrange */
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(new WriteContext(1L, SecurityPermission.authorities()));
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.deactivate(1L));

        /* Assert */
        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SECURITY_SELF_DEACTIVATION);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verifyNoInteractions(mapper, branches);
        verifyNoMoreInteractions(users, mapper, branches, gate);
    }

    @Test
    void shouldRejectDeactivationWhenTargetIsInactive() throws Exception {
        /* Arrange */
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(createContext());
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        user.setActive(false);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.deactivate(1L));

        /* Assert */
        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_INACTIVE);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verifyNoInteractions(mapper, branches);
        verifyNoMoreInteractions(users, mapper, branches, gate);
    }

    @Test
    void shouldRejectDeactivationWhenTargetIsSuperior() throws Exception {
        /* Arrange */
        User user = createUser();
        when(gate.begin(USERS_WRITE)).thenReturn(new WriteContext(99L, Set.of("security:users:write")));
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.deactivate(1L));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
        verify(gate).begin(USERS_WRITE);
        verify(users).findByIdAndDeletedFalse(1L);
        verifyNoInteractions(mapper, branches);
        verifyNoMoreInteractions(users, mapper, branches, gate);
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

}