package pe.sotomayor.creditos.security.application.mapper.branch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import pe.sotomayor.creditos.security.application.dto.branch.request.CreateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.request.UpdateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;
import pe.sotomayor.creditos.security.domain.model.entity.Branch;

/**
 * Pruebas unitarias para {@link BranchMapper}.
 *
 * <p>
 * Verifica las conversiones entre los DTO de aplicación y la entidad
 * {@link Branch}.
 * </p>
 *
 * <p>
 * Estas pruebas validan el contrato del mapper generado por MapStruct.
 * </p>
 *
 * @author DarwinG
 */
class BranchMapperTest {

    private final BranchMapper branchMapper =
            Mappers.getMapper(BranchMapper.class);

    @Test
    @DisplayName("Debe convertir CreateBranchRequest en Branch")
    void shouldMapCreateRequestToEntity() {

        /* Arrange */

        CreateBranchRequest request = createRequest();

        /* Act */

        Branch branch = branchMapper.toEntity(request);

        /* Assert */

        assertNotNull(branch);

        assertEquals(request.code(), branch.getCode());
        assertEquals(request.name(), branch.getName());
        assertEquals(request.address(), branch.getAddress());
        assertEquals(request.phone(), branch.getPhone());
        assertEquals(request.email(), branch.getEmail());
    }

    @Test
    @DisplayName("Debe actualizar una sucursal existente")
    void shouldUpdateEntity() {

        /* Arrange */

        Branch branch = createBranch();

        UpdateBranchRequest request = updateRequest();

        /* Act */

        branchMapper.updateEntity(request, branch);

        /* Assert */

        assertEquals(request.name(), branch.getName());
        assertEquals(request.address(), branch.getAddress());
        assertEquals(request.phone(), branch.getPhone());
        assertEquals(request.email(), branch.getEmail());
    }

    @Test
    @DisplayName("Debe convertir Branch en BranchResponse")
    void shouldMapEntityToResponse() {

        /* Arrange */

        Branch branch = createBranch();

        /* Act */

        BranchResponse response = branchMapper.toResponse(branch);

        /* Assert */

        assertNotNull(response);

        assertEquals(branch.getId(), response.id());
        assertEquals(branch.getCode(), response.code());
        assertEquals(branch.getName(), response.name());
        assertEquals(branch.getAddress(), response.address());
        assertEquals(branch.getPhone(), response.phone());
        assertEquals(branch.getEmail(), response.email());
        assertEquals(branch.getManagerId(), response.managerId());
        assertEquals(branch.getActive(), response.active());
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

    private UpdateBranchRequest updateRequest() {

        return new UpdateBranchRequest(
                "Sucursal Nueva",
                "Av. Central 456",
                "999888777",
                "nueva@creditos.com",
                null
        );

    }

    private Branch createBranch() {

        Branch branch = new Branch();

        branch.setId(1L);
        branch.setCode("TOC01");
        branch.setName("Sucursal Tocache");
        branch.setAddress("Av. Principal 123");
        branch.setPhone("987654321");
        branch.setEmail("tocache@creditos.com");
        branch.setManagerId(null);
        branch.setActive(true);

        return branch;
    }

}
