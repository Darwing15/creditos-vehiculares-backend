package pe.sotomayor.creditos.security.application.usecase.branch;

import pe.sotomayor.creditos.security.application.dto.branch.request.CreateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

/**
 * Define el caso de uso para registrar nuevas sucursales.
 *
 * <p>
 * Este contrato abstrae la operación de creación de sucursales
 * dentro del módulo de seguridad.
 * </p>
 *
 * @author DarwinG
 */
public interface CreateBranchUseCase {

    /**
     * Registra una nueva sucursal en el sistema.
     *
     * @param request información requerida para crear la sucursal
     * @return información de la sucursal creada
     */
    BranchResponse create(CreateBranchRequest request);

}
