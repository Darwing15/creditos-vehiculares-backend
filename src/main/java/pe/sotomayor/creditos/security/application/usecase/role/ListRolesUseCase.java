package pe.sotomayor.creditos.security.application.usecase.role;

import java.util.List;

import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

/**
 * Define el caso de uso para listar los roles disponibles del sistema.
 *
 * <p>
 * Este contrato abstrae la operación de consulta múltiple de roles,
 * permitiendo mantener desacoplada la capa de aplicación respecto
 * a la implementación de persistencia.
 * </p>
 *
 * @author DarwinG
 */
public interface ListRolesUseCase {

    /**
     * Obtiene todos los roles disponibles del sistema.
     *
     * @return lista de roles registrados
     */
    List<RoleResponse> findAll();

}
