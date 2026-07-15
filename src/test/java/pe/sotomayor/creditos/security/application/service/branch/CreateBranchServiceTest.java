package pe.sotomayor.creditos.security.application.service.branch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;
import pe.sotomayor.creditos.security.application.mapper.branch.BranchMapper;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.repository.BranchRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;

import pe.sotomayor.creditos.security.application.dto.branch.request.CreateBranchRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link CreateBranchService}.
 *
 * <p>
 * Verifica el comportamiento del caso de uso encargado de registrar
 * nuevas sucursales dentro del sistema.
 * </p>
 *
 * @author DarwinG
 */
@ExtendWith(MockitoExtension.class)
class CreateBranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchMapper branchMapper;

    @InjectMocks
    private CreateBranchService createBranchService;

    @Test
    @DisplayName("Debe crear una sucursal cuando el código no existe")
    void shouldCreateBranchWhenCodeDoesNotExist() {

        /* Arrange */

        CreateBranchRequest request = createRequest();

        Branch branch = createBranch(1L);

        BranchResponse expectedResponse = createResponse(1L);

        when(branchRepository.existsByCode(
                request.code()))
                .thenReturn(false);

        when(branchMapper.toEntity(request))
                .thenReturn(branch);

        when(branchRepository.save(branch))
                .thenReturn(branch);

        when(branchMapper.toResponse(branch))
                .thenReturn(expectedResponse);

        /* Act */

        BranchResponse response = createBranchService.create(request);

        /* Assert */

        assertNotNull(response);

        assertEquals(
                1L,
                response.id()
        );

        assertEquals(
                "TOC01",
                response.code()
        );

        assertEquals(
                "Sucursal Tocache",
                response.name()
        );

        verify(branchRepository).existsByCode(request.code());
        verify(branchMapper).toEntity(request);
        verify(branchRepository).save(branch);
        verify(branchMapper).toResponse(branch);

        verifyNoMoreInteractions(
                branchRepository,
                branchMapper
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el código de sucursal ya existe")
    void shouldThrowConflictExceptionWhenCodeAlreadyExists() {

        /* Arrange */

        CreateBranchRequest request =
                new CreateBranchRequest(
                        "TOC01",
                        "Sucursal Tocache",
                        "Av. Principal 123",
                        "987654321",
                        "tocache@creditos.com",
                        null
                );

        when(branchRepository.existsByCode(
                request.code()))
                .thenReturn(true);

        /* Act & Assert */

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                createBranchService.create(request)
                );

        assertEquals(
                ErrorCode.BRANCH_ALREADY_EXISTS,
                exception.getErrorCode()
        );

        verify(branchRepository).existsByCode(request.code());

        verifyNoMoreInteractions(
                branchRepository,
                branchMapper
        );
    }

    private CreateBranchRequest createRequest() {
        return new CreateBranchRequest(
                "TOC01",
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
