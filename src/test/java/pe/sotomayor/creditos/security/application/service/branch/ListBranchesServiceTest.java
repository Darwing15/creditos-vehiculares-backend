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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link ListBranchesService}.
 *
 * <p>
 * Verifica el comportamiento del caso de uso encargado de consultar
 * las sucursales registradas dentro del sistema.
 * </p>
 *
 * @author DarwinG
 */
@ExtendWith(MockitoExtension.class)
class ListBranchesServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchMapper branchMapper;

    @InjectMocks
    private ListBranchesService listBranchesService;

    @Test
    @DisplayName("Debe listar sucursales existentes correctamente")
    void shouldReturnBranchesWhenBranchesExist() {

        /* Arrange */

        Branch tocache =
                createBranch(
                        1L,
                        "TOC01",
                        "Sucursal Tocache"
                );

        Branch tarapoto =
                createBranch(
                        2L,
                        "TAR01",
                        "Sucursal Tarapoto"
                );

        List<Branch> branches = List.of(
                tocache,
                tarapoto
        );

        BranchResponse tocacheResponse =
                createResponse(
                        1L,
                        "TOC01",
                        "Sucursal Tocache"
                );

        BranchResponse tarapotoResponse =
                createResponse(
                        2L,
                        "TAR01",
                        "Sucursal Tarapoto"
                );

        when(branchRepository.findAll())
                .thenReturn(branches);

        when(branchMapper.toResponse(tocache))
                .thenReturn(tocacheResponse);

        when(branchMapper.toResponse(tarapoto))
                .thenReturn(tarapotoResponse);

        /* Act */

        List<BranchResponse> result =
                listBranchesService.findAll();

        /* Assert */

        assertNotNull(result);

        assertEquals(
                2,
                result.size()
        );

        assertEquals(
                "TOC01",
                result.get(0).code()
        );

        assertEquals(
                "TAR01",
                result.get(1).code()
        );

        verify(branchRepository).findAll();
        verify(branchMapper).toResponse(tocache);
        verify(branchMapper).toResponse(tarapoto);

        verifyNoMoreInteractions(
                branchRepository,
                branchMapper
        );
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no existen sucursales")
    void shouldReturnEmptyListWhenNoBranchesExist() {

        /* Arrange */

        when(branchRepository.findAll()).thenReturn(List.of());

        /* Act */

        List<BranchResponse> result = listBranchesService.findAll();

        /* Assert */

        assertNotNull(result);

        assertEquals(
                0,
                result.size()
        );

        verify(branchRepository).findAll();

        verifyNoMoreInteractions(
                branchRepository,
                branchMapper
        );
    }

    private Branch createBranch(
            Long id,
            String code,
            String name) {

        Branch branch = new Branch();

        branch.setId(id);

        branch.setCode(code);

        branch.setName(name);

        branch.setAddress("Av. Principal 123");

        branch.setPhone("987654321");

        branch.setEmail("contacto@creditos.com");

        branch.setActive(true);

        return branch;
    }

    private BranchResponse createResponse(
            Long id,
            String code,
            String name) {

        return new BranchResponse(
                id,
                code,
                name,
                "Av. Principal 123",
                "987654321",
                "contacto@creditos.com",
                null,
                true
        );
    }

}