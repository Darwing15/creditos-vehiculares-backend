package pe.sotomayor.creditos.security.application.service.branch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.security.application.dto.branch.request.UpdateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

import pe.sotomayor.creditos.security.application.mapper.branch.BranchMapper;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;

import pe.sotomayor.creditos.security.domain.repository.BranchRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateBranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchMapper branchMapper;

    @InjectMocks
    private UpdateBranchService updateBranchService;

    @Test
    void shouldUpdateBranchWhenExists() {

        /* Arrange */

        Long branchId = 1L;

        UpdateBranchRequest request = createRequest();

        Branch branch = createBranch(branchId);

        BranchResponse response = createResponse(branchId);

        when(branchRepository.findById(branchId))
                .thenReturn(Optional.of(branch));

        when(branchRepository.save(branch))
                .thenReturn(branch);

        when(branchMapper.toResponse(branch))
                .thenReturn(response);

        /* Act */

        BranchResponse result =
                updateBranchService.update(
                        branchId,
                        request
                );

        /* Assert */

        assertNotNull(result);

        assertEquals(
                branchId,
                result.id()
        );

        assertEquals(
                "Sucursal Tocache",
                result.name()
        );

        verify(branchRepository).findById(branchId);

        verify(branchMapper)
                .updateEntity(
                        request,
                        branch
                );

        verify(branchRepository).save(branch);
        verify(branchMapper).toResponse(branch);

        verifyNoMoreInteractions(
                branchRepository,
                branchMapper
        );
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenBranchDoesNotExist() {

        /* Arrange */

        Long branchId = 1L;

        UpdateBranchRequest request = createRequest();

        when(branchRepository.findById(branchId))
                .thenReturn(Optional.empty());

        /* Act & Assert */

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                updateBranchService.update(
                                        branchId,
                                        request
                                )
                );

        assertEquals(
                ErrorCode.BRANCH_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(branchRepository).findById(branchId);

        verifyNoMoreInteractions(
                branchRepository,
                branchMapper
        );
    }

    private UpdateBranchRequest createRequest() {

        return new UpdateBranchRequest(
                "Sucursal Tocache",
                "Av. Principal 123",
                "987654321",
                "tocache@creditos.com",
                null
        );
    }

    private Branch createBranch(Long id) {

        Branch branch = new Branch();

        branch.setId(id);
        branch.setCode("TOC01");
        branch.setName("Sucursal Tocache");
        branch.setAddress("Av. Principal 123");
        branch.setPhone("987654321");
        branch.setEmail("tocache@creditos.com");

        return branch;
    }

    private BranchResponse createResponse(Long id) {

        return new BranchResponse(
                id,
                "TOC01",
                "Sucursal Tocache",
                "Av. Principal 123",
                "987654321",
                "tocache@creditos.com",
                null,
                true
        );
    }

}
