package pe.sotomayor.creditos.security.application.usecase.branch;

import java.util.List;

import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

/**
 * Define el caso de uso para consultar las sucursales registradas
 * dentro del sistema.
 *
 * <p>
 * Este contrato abstrae la operación de consulta de sucursales,
 * permitiendo que la capa de aplicación permanezca desacoplada
 * de la implementación de persistencia.
 * </p>
 *
 * @author DarwinG
 */
public interface ListBranchesUseCase {

    /**
     * Obtiene todas las sucursales registradas.
     *
     * @return lista de sucursales
     */
    List<BranchResponse> findAll();

}
