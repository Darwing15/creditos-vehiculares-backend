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

import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;
import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate.WriteContext;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;

import pe.sotomayor.creditos.security.application.dto.branch.request.CreateBranchRequest;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission.BRANCHES_WRITE;

@ExtendWith(MockitoExtension.class)
class CreateBranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchMapper branchMapper;

    @Mock
    private SecurityWriteGate gate;

    @InjectMocks
    private CreateBranchService service;

    @Test
    void shouldCreateBranchWhenDataIsValid() throws Exception {
        /* Arrange */
        CreateBranchRequest request = createRequest();
        Branch entity = createBranch();
        when(gate.begin(BRANCHES_WRITE)).thenReturn(createContext());
        when(branchRepository.existsByCode(request.code())).thenReturn(false);
        BranchResponse expected = createResponse();
        when(branchMapper.toEntity(request)).thenReturn(entity);
        when(branchRepository.saveAndFlush(entity)).thenReturn(entity);
        when(branchMapper.toResponse(entity)).thenReturn(expected);

        /* Act */
        BranchResponse result = service.create(request);

        /* Assert */
        assertThat(result).isEqualTo(expected);
        assertThat(entity.getActive()).isTrue();

        verify(gate).begin(BRANCHES_WRITE);
        verify(branchRepository).existsByCode(request.code());
        verify(branchMapper).toEntity(request);
        verify(branchRepository).saveAndFlush(entity);
        verify(gate).finish();
        verify(branchMapper).toResponse(entity);
        verifyNoMoreInteractions(branchRepository, branchMapper, gate);
    }

    @Test
    void shouldStopBeforeReadingDataWhenWriteGateDeniesAccess() throws Exception {
        /* Arrange */
        CreateBranchRequest request = createRequest();
        when(gate.begin(BRANCHES_WRITE)).thenThrow(new AccessDeniedException("denied"));

        /* Act */
        Throwable thrown = catchThrowable(() -> service.create(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        verify(gate).begin(BRANCHES_WRITE);
        verifyNoInteractions(branchRepository, branchMapper);
        verifyNoMoreInteractions(branchRepository, branchMapper, gate);
    }

    @Test
    void shouldRejectDuplicateWhenCodeAlreadyExists() throws Exception {
        /* Arrange */
        CreateBranchRequest request = createRequest();
        Branch entity = createBranch();
        when(gate.begin(BRANCHES_WRITE)).thenReturn(createContext());
        when(branchRepository.existsByCode(request.code())).thenReturn(true);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.create(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ConflictException.class).extracting("errorCode").isEqualTo(ErrorCode.BRANCH_ALREADY_EXISTS);
        verify(gate).begin(BRANCHES_WRITE);
        verify(branchRepository).existsByCode(request.code());
        verifyNoInteractions(branchMapper);
        verifyNoMoreInteractions(branchRepository, branchMapper, gate);
    }

    @Test
    void shouldRejectCreationWhenManagerIsProvided() throws Exception {
        /* Arrange */
        CreateBranchRequest valid = createRequest();
        CreateBranchRequest request = new CreateBranchRequest(valid.code(), valid.name(), valid.address(), valid.phone(), valid.email(), 1L);
        Branch entity = createBranch();
        when(gate.begin(BRANCHES_WRITE)).thenReturn(createContext());
        when(branchRepository.existsByCode(request.code())).thenReturn(false);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.create(request));

        /* Assert */
        assertThat(thrown).isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(gate).begin(BRANCHES_WRITE);
        verify(branchRepository).existsByCode(request.code());
        verifyNoInteractions(branchMapper);
        verifyNoMoreInteractions(branchRepository, branchMapper, gate);
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

    private CreateBranchRequest createRequest() {
        return new CreateBranchRequest(
                "LIM",
                "Lima",
                "Av. Peru 123",
                "999999999",
                "lima@test.com",
                null
        );
    }

}
