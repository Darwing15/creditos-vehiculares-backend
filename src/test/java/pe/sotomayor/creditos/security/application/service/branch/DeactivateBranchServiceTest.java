package pe.sotomayor.creditos.security.application.service.branch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;
import pe.sotomayor.creditos.security.application.mapper.branch.BranchMapper;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;
import pe.sotomayor.creditos.security.domain.repository.BranchRepository;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;
import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate.WriteContext;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission.BRANCHES_WRITE;

@ExtendWith(MockitoExtension.class)
class DeactivateBranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchMapper branchMapper;

    @Mock
    private SecurityWriteGate gate;

    @Mock
    private UserRepository users;

    @InjectMocks
    private DeactivateBranchService service;

    @Test
    void shouldDeactivateBranchWhenDataIsValid() throws Exception {
        /* Arrange */
        Branch entity = createBranch();
        when(gate.begin(BRANCHES_WRITE)).thenReturn(createContext());
        when(branchRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(entity));
        when(users.existsByBranchId(3L)).thenReturn(false);
        BranchResponse expected = createResponse();
        when(branchRepository.saveAndFlush(entity)).thenReturn(entity);
        when(branchMapper.toResponse(entity)).thenReturn(expected);

        /* Act */
        BranchResponse result = service.deactivate(3L);

        /* Assert */
        assertThat(result).isEqualTo(expected);
        assertThat(entity.getActive()).isFalse();
        verify(gate).begin(BRANCHES_WRITE);
        verify(branchRepository).findByIdAndDeletedFalse(3L);
        verify(users).existsByBranchId(3L);
        verify(branchRepository).saveAndFlush(entity);
        verify(gate).finish();
        verify(branchMapper).toResponse(entity);
        verifyNoMoreInteractions(branchRepository, branchMapper, gate, users);
    }

    @Test
    void shouldStopBeforeReadingDataWhenWriteGateDeniesAccess() throws Exception {
        /* Arrange */
        when(gate.begin(BRANCHES_WRITE)).thenThrow(new AccessDeniedException("denied"));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.deactivate(3L));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        verify(gate).begin(BRANCHES_WRITE);
        verifyNoInteractions(branchRepository, branchMapper, users);
        verifyNoMoreInteractions(branchRepository, branchMapper, gate, users);
    }

    @Test
    void shouldThrowNotFoundWhenBranchDoesNotExist() throws Exception {
        /* Arrange */
        when(gate.begin(BRANCHES_WRITE)).thenReturn(createContext());
        when(branchRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(() -> service.deactivate(3L));

        /* Assert */
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).extracting("errorCode").isEqualTo(ErrorCode.BRANCH_NOT_FOUND);
        verify(gate).begin(BRANCHES_WRITE);
        verify(branchRepository).findByIdAndDeletedFalse(3L);
        verifyNoInteractions(branchMapper, users);
        verifyNoMoreInteractions(branchRepository, branchMapper, gate, users);
    }

    @Test
    void shouldRejectDeactivationWhenUsersRemainAssigned() throws Exception {
        /* Arrange */
        Branch entity = createBranch();
        when(gate.begin(BRANCHES_WRITE)).thenReturn(createContext());
        when(branchRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(entity));
        when(users.existsByBranchId(3L)).thenReturn(true);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.deactivate(3L));

        /* Assert */
        assertThat(thrown).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BRANCH_IN_USE);
        verify(gate).begin(BRANCHES_WRITE);
        verify(branchRepository).findByIdAndDeletedFalse(3L);
        verify(users).existsByBranchId(3L);
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

}

