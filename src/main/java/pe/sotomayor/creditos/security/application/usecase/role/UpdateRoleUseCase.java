package pe.sotomayor.creditos.security.application.usecase.role;

import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

/**
 * Define el caso de uso para actualizar un rol existente.
 *
 * <p>
 * Este contrato abstrae la operación de modificación de un rol, permitiendo que
 * la implementación permanezca desacoplada de las capas consumidoras.
 * </p>
 *
 * @author DarwinG
 */
public interface UpdateRoleUseCase {

    /**
     * Actualiza la información de un rol existente.
     *
     * @param id identificador único del rol
     * @param request datos actualizados del rol
     * @return información del rol actualizado
     */
    RoleResponse update(Long id, UpdateRoleRequest request);

}
