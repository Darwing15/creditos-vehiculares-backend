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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListBranchesServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchMapper branchMapper;

    @InjectMocks
    private ListBranchesService service;

    @Test
    void shouldMapResultsWhenRecordsExist() throws Exception {
        /* Arrange */
        Branch entity = createBranch();
        BranchResponse expected = createResponse();
        when(branchRepository.findAllByDeletedFalse()).thenReturn(List.of(entity));
        when(branchMapper.toResponse(entity)).thenReturn(expected);

        /* Act */
        List<BranchResponse> result = service.findAll();

        /* Assert */
        assertThat(result).containsExactly(expected);
        verify(branchRepository).findAllByDeletedFalse();
        verify(branchMapper).toResponse(entity);
        verifyNoMoreInteractions(branchRepository, branchMapper);
    }

    @Test
    void shouldReturnEmptyListWhenNoRecordsExist() throws Exception {
        /* Arrange */
        when(branchRepository.findAllByDeletedFalse()).thenReturn(List.of());

        /* Act */
        List<BranchResponse> result = service.findAll();

        /* Assert */
        assertThat(result).isEmpty();
        verify(branchRepository).findAllByDeletedFalse();
        verifyNoInteractions(branchMapper);
        verifyNoMoreInteractions(branchRepository, branchMapper);
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

}