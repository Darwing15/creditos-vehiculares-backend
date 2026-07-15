package pe.sotomayor.creditos.security.application.usecase.branch;

import pe.sotomayor.creditos.security.application.dto.branch.request.UpdateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

/**
 * Define el caso de uso para actualizar sucursales existentes.
 *
 * <p>
 * Este contrato abstrae la operación de modificación de información
 * administrativa de una sucursal dentro del módulo de seguridad.
 * </p>
 *
 * @author DarwinG
 */
public interface UpdateBranchUseCase {

    /**
     * Actualiza la información de una sucursal existente.
     *
     * @param id      identificador único de la sucursal
     * @param request información actualizada de la sucursal
     * @return información de la sucursal actualizada
     */
    BranchResponse update(Long id, UpdateBranchRequest request);

}
