package pe.sotomayor.creditos.security.application.service.branch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;
import pe.sotomayor.creditos.security.application.mapper.branch.BranchMapper;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;

import pe.sotomayor.creditos.security.domain.repository.BranchRepository;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeactivateBranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BranchMapper branchMapper;

    @InjectMocks
    private DeactivateBranchService deactivateBranchService;

    @Test
    void shouldDeactivateBranchWhenBranchExistsAndHasNoUsers() {

        /* Arrange */

        Long branchId = 1L;

        Branch branch = createBranch(branchId);

        BranchResponse expectedResponse =
                createResponse(branchId);

        when(branchRepository.findById(branchId))
                .thenReturn(Optional.of(branch));

        when(userRepository.existsByBranchId(branchId))
                .thenReturn(false);

        when(branchRepository.save(branch))
                .thenReturn(branch);

        when(branchMapper.toResponse(branch))
                .thenReturn(expectedResponse);

        /* Act */

        BranchResponse response =
                deactivateBranchService.deactivate(branchId);

        /* Assert */

        assertNotNull(response);

        assertEquals(
                branchId,
                response.id()
        );

        assertEquals(
                "TOC01",
                response.code()
        );

        assertEquals(
                false,
                branch.getActive()
        );

        verify(branchRepository).findById(branchId);
        verify(userRepository).existsByBranchId(branchId);
        verify(branchRepository).save(branch);
        verify(branchMapper).toResponse(branch);

        verifyNoMoreInteractions(
                branchRepository,
                userRepository,
                branchMapper
        );
    }

    @Test
    void shouldThrowConflictExceptionWhenBranchHasUsers() {

        /* Arrange */

        Long branchId = 1L;

        Branch branch = createBranch(branchId);

        when(branchRepository.findById(branchId))
                .thenReturn(Optional.of(branch));

        when(userRepository.existsByBranchId(branchId))
                .thenReturn(true);

        /* Act & Assert */

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                deactivateBranchService.deactivate(branchId)
                );

        assertEquals(
                ErrorCode.BRANCH_IN_USE,
                exception.getErrorCode()
        );

        verify(branchRepository).findById(branchId);
        verify(userRepository).existsByBranchId(branchId);

        verifyNoMoreInteractions(
                branchRepository,
                userRepository,
                branchMapper
        );
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenBranchDoesNotExist() {

        /* Arrange */

        Long branchId = 1L;

        when(branchRepository.findById(branchId))
                .thenReturn(Optional.empty());

        /* Act & Assert */

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                deactivateBranchService.deactivate(branchId)
                );

        assertEquals(
                ErrorCode.BRANCH_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(branchRepository).findById(branchId);

        verifyNoMoreInteractions(
                branchRepository,
                userRepository,
                branchMapper
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

        branch.setActive(true);

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
                false
        );
    }

}

