package pe.sotomayor.creditos.security.application.service.branch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import pe.sotomayor.creditos.security.application.dto.branch.request.UpdateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

import pe.sotomayor.creditos.security.application.mapper.branch.BranchMapper;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;

import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;
import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;
import pe.sotomayor.creditos.security.domain.repository.BranchRepository;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;
import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;
import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate.WriteContext;
import pe.sotomayor.creditos.shared.error.ErrorCode;
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

import static pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission.BRANCHES_WRITE;

@ExtendWith(MockitoExtension.class)
class UpdateBranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchMapper branchMapper;

    @Mock
    private SecurityWriteGate gate;

    @Mock
    private UserRepository users;

    @InjectMocks
    private UpdateBranchService service;

    @Test
    void shouldUpdateBranchWhenDataIsValid() throws Exception {
        /* Arrange */
        UpdateBranchRequest request = createRequest();
        Branch entity = createBranch();
        when(gate.begin(BRANCHES_WRITE)).thenReturn(createContext());
        when(branchRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(entity));
        BranchResponse expected = createResponse();
        when(branchRepository.saveAndFlush(entity)).thenReturn(entity);
        when(branchMapper.toResponse(entity)).thenReturn(expected);

        /* Act */
        BranchResponse result = service.update(3L, request);

        /* Assert */
        assertThat(result).isEqualTo(expected);

        verify(gate).begin(BRANCHES_WRITE);
        verify(branchRepository).findByIdAndDeletedFalse(3L);
        verify(branchMapper).updateEntity(request, entity);
        verify(branchRepository).saveAndFlush(entity);
        verify(gate).finish();
        verify(branchMapper).toResponse(entity);
        verifyNoMoreInteractions(branchRepository, branchMapper, gate, users);
    }

    @Test
    void shouldStopBeforeReadingDataWhenWriteGateDeniesAccess() throws Exception {
        /* Arrange */
        UpdateBranchRequest request = createRequest();
        when(gate.begin(BRANCHES_WRITE)).thenThrow(new AccessDeniedException("denied"));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(3L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        verify(gate).begin(BRANCHES_WRITE);
        verifyNoInteractions(branchRepository, branchMapper, users);
        verifyNoMoreInteractions(branchRepository, branchMapper, gate, users);
    }

    @Test
    void shouldThrowNotFoundWhenBranchDoesNotExist() throws Exception {
        /* Arrange */
        UpdateBranchRequest request = createRequest();
        when(gate.begin(BRANCHES_WRITE)).thenReturn(createContext());
        when(branchRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(3L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).extracting("errorCode").isEqualTo(ErrorCode.BRANCH_NOT_FOUND);
        verify(gate).begin(BRANCHES_WRITE);
        verify(branchRepository).findByIdAndDeletedFalse(3L);
        verifyNoInteractions(branchMapper, users);
        verifyNoMoreInteractions(branchRepository, branchMapper, gate, users);
    }

    @Test
    void shouldAcceptManagerWhenActiveInSameBranch() throws Exception {
        /* Arrange */
        UpdateBranchRequest valid = createRequest();
        UpdateBranchRequest request = new UpdateBranchRequest(valid.name(), valid.address(), valid.phone(), valid.email(), 1L);
        Branch entity = createBranch();
        when(gate.begin(BRANCHES_WRITE)).thenReturn(createContext());
        when(branchRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(entity));
        User manager = createUser();
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(manager));
        BranchResponse expected = createResponse();
        when(branchRepository.saveAndFlush(entity)).thenReturn(entity);
        when(branchMapper.toResponse(entity)).thenReturn(expected);

        /* Act */
        BranchResponse result = service.update(3L, request);

        /* Assert */
        assertThat(result).isEqualTo(expected);
        verify(gate).begin(BRANCHES_WRITE);
        verify(branchRepository).findByIdAndDeletedFalse(3L);
        verify(users).findByIdAndDeletedFalse(1L);
        verify(branchMapper).updateEntity(request, entity);
        verify(branchRepository).saveAndFlush(entity);
        verify(branchMapper).toResponse(entity);
        verify(gate).finish();
        verifyNoMoreInteractions(branchRepository, branchMapper, gate, users);
    }

    @Test
    void shouldRejectManagerWhenMissing() throws Exception {
        /* Arrange */
        UpdateBranchRequest valid = createRequest();
        UpdateBranchRequest request = new UpdateBranchRequest(valid.name(), valid.address(), valid.phone(), valid.email(), 1L);
        Branch entity = createBranch();
        when(gate.begin(BRANCHES_WRITE)).thenReturn(createContext());
        when(branchRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(entity));
        User manager = createUser();
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(3L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(gate).begin(BRANCHES_WRITE);
        verify(branchRepository).findByIdAndDeletedFalse(3L);
        verify(users).findByIdAndDeletedFalse(1L);
        verifyNoInteractions(branchMapper);
        verifyNoMoreInteractions(branchRepository, branchMapper, gate, users);
    }

    @Test
    void shouldRejectManagerWhenInactive() throws Exception {
        /* Arrange */
        UpdateBranchRequest valid = createRequest();
        UpdateBranchRequest request = new UpdateBranchRequest(valid.name(), valid.address(), valid.phone(), valid.email(), 1L);
        Branch entity = createBranch();
        when(gate.begin(BRANCHES_WRITE)).thenReturn(createContext());
        when(branchRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(entity));
        User manager = createUser();
        manager.setActive(false);
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(manager));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(3L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(gate).begin(BRANCHES_WRITE);
        verify(branchRepository).findByIdAndDeletedFalse(3L);
        verify(users).findByIdAndDeletedFalse(1L);
        verifyNoInteractions(branchMapper);
        verifyNoMoreInteractions(branchRepository, branchMapper, gate, users);
    }

    @Test
    void shouldRejectManagerWhenOtherBranch() throws Exception {
        /* Arrange */
        UpdateBranchRequest valid = createRequest();
        UpdateBranchRequest request = new UpdateBranchRequest(valid.name(), valid.address(), valid.phone(), valid.email(), 1L);
        Branch entity = createBranch();
        when(gate.begin(BRANCHES_WRITE)).thenReturn(createContext());
        when(branchRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(entity));
        User manager = createUser();
        manager.getBranch().setId(4L);
        when(users.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(manager));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.update(3L, request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(gate).begin(BRANCHES_WRITE);
        verify(branchRepository).findByIdAndDeletedFalse(3L);
        verify(users).findByIdAndDeletedFalse(1L);
        verifyNoInteractions(branchMapper);
        verifyNoMoreInteractions(branchRepository, branchMapper, gate, users);
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

    private BranchResponse createResponse() {
        return new BranchResponse(
                3L,
                "LIM",
                "Lima",
                "Av. Peru 123",
                "999999999",
                "lima@test.com",
                null,
                true
        );
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

    private UpdateBranchRequest createRequest() {
        return new UpdateBranchRequest(
                "Lima",
                "Av. Peru 123",
                "999999999",
                "lima@test.com",
                null
        );
    }

}
