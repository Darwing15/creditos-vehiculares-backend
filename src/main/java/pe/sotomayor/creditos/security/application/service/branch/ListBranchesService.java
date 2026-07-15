package pe.sotomayor.creditos.security.application.service.branch;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

import pe.sotomayor.creditos.security.application.mapper.branch.BranchMapper;

import pe.sotomayor.creditos.security.application.usecase.branch.ListBranchesUseCase;

import pe.sotomayor.creditos.security.domain.repository.BranchRepository;

/**
 * Implementa el caso de uso para consultar sucursales registradas.
 *
 * <p>
 * Recupera las sucursales existentes y transforma las entidades
 * del dominio en objetos de respuesta utilizados por la capa
 * de aplicación.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListBranchesService implements ListBranchesUseCase {

    private final BranchRepository branchRepository;

    private final BranchMapper branchMapper;

    /**
     * Obtiene todas las sucursales registradas.
     *
     * @return lista de sucursales
     */
    @Override
    public List<BranchResponse> findAll() {

        return branchRepository.findAll()
                .stream()
                .map(branchMapper::toResponse)
                .toList();

    }

}
