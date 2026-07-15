package pe.sotomayor.creditos.security.application.usecase.role;

import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

/**
 * Define el caso de uso para retirar un rol existente del sistema.
 *
 * <p>
 * Este contrato abstrae la operación de eliminación lógica de un rol,
 * conservando el historial del registro dentro del sistema.
 * </p>
 *
 * @author DarwinG
 */
public interface DeactivateRoleUseCase {

    /**
     * Realiza la eliminación lógica de un rol mediante su identificador.
     *
     * @param id identificador único del rol
     * @return información del rol retirado del sistema
     */
    RoleResponse deactivate(Long id);

}
