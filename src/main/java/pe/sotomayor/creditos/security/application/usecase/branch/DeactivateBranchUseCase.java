package pe.sotomayor.creditos.security.application.usecase.branch;

import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

/**
 * Define el caso de uso para desactivar sucursales existentes.
 *
 * <p>
 * Este contrato abstrae la operación de cambio de estado operativo
 * de una sucursal dentro del módulo de seguridad.
 * </p>
 *
 * @author DarwinG
 */
public interface DeactivateBranchUseCase {

    /**
     * Desactiva una sucursal existente.
     *
     * @param id identificador único de la sucursal
     *
     * @return información de la sucursal desactivada
     */
    BranchResponse deactivate(Long id);

}
