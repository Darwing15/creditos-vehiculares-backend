package pe.sotomayor.creditos.security.application.usecase.role;

import pe.sotomayor.creditos.security.application.dto.role.request.CreateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

/**
 * Define el caso de uso para registrar un nuevo rol en el sistema.
 *
 * <p>
 * Este contrato establece la operación necesaria para crear un rol,
 * desacoplando la capa de presentación de la implementación del servicio.
 * </p>
 *
 * @author DarwinG
 */
public interface CreateRoleUseCase {

    /**
     * Registra un nuevo rol en el sistema.
     *
     * @param request información del rol a registrar
     * @return información del rol creado
     */
    RoleResponse create(CreateRoleRequest request);

}
