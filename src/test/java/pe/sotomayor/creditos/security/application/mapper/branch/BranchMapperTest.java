package pe.sotomayor.creditos.security.application.mapper.branch;

import org.junit.jupiter.api.Test;

import org.mapstruct.factory.Mappers;

import pe.sotomayor.creditos.security.application.dto.branch.request.CreateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.request.UpdateBranchRequest;

import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;

import static org.assertj.core.api.Assertions.assertThat;

class BranchMapperTest {

    private final BranchMapper mapper =
            Mappers.getMapper(BranchMapper.class);

    @Test
    void shouldMapPublicFieldsWhenCreatingEntity() throws Exception {
        /* Arrange */
        CreateBranchRequest request = createRequest();

        /* Act */
        Branch result = mapper.toEntity(request);

        /* Assert */
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        assertThat(result.getCode()).isEqualTo(request.code());
        assertThat(result.getName()).isEqualTo(request.name());
        assertThat(result.getAddress()).isEqualTo(request.address());
        assertThat(result.getPhone()).isEqualTo(request.phone());
        assertThat(result.getEmail()).isEqualTo(request.email());
        assertThat(result.getManagerId()).isNull();
    }

    @Test
    void shouldPreserveProtectedFieldsWhenUpdatingEntity() throws Exception {
        /* Arrange */
        Branch entity = createBranch();
        entity.setRowVersion(7L);
        UpdateBranchRequest request = createUpdateRequest();

        /* Act */
        mapper.updateEntity(request, entity);

        /* Assert */
        assertThat(entity.getId()).isEqualTo(3L);
        assertThat(entity.getRowVersion()).isEqualTo(7L);
        assertThat(entity.getName()).isEqualTo(request.name());
        assertThat(entity.getAddress()).isEqualTo(request.address());
        assertThat(entity.getPhone()).isEqualTo(request.phone());
        assertThat(entity.getEmail()).isEqualTo(request.email());
        assertThat(entity.getManagerId()).isEqualTo(request.managerId());
        assertThat(entity.getCode()).isEqualTo("LIM");
        assertThat(entity.getActive()).isTrue();
    }

    @Test
    void shouldMapResponseWhenEntityHasRelations() throws Exception {
        /* Arrange */
        Branch entity = createBranch();

        /* Act */
        BranchResponse result = mapper.toResponse(entity);

        /* Assert */
        assertThat(result).isEqualTo(createResponse());
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

    private UpdateBranchRequest createUpdateRequest() {
        return new UpdateBranchRequest(
                "Lima Centro",
                "Av. Peru 456",
                "988888888",
                "centro@test.com",
                1L
        );
    }

}
