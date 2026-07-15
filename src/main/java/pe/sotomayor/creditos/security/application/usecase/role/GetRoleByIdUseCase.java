package pe.sotomayor.creditos.security.application.usecase.role;

import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

/**
 * Define el caso de uso para obtener un rol mediante su identificador.
 *
 * <p>
 * Este contrato abstrae la operación de consulta de un rol, permitiendo que su
 * implementación permanezca desacoplada de las capas consumidoras.
 * </p>
 *
 * @author DarwinG
 */
public interface GetRoleByIdUseCase {

    /**
     * Obtiene un rol a partir de su identificador.
     *
     * @param id identificador único del rol
     * @return información del rol encontrado
     */
    RoleResponse getById(Long id);

}
