package pe.sotomayor.creditos.security.application.usecase.user;

import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import java.util.List;

/**
 * Define el caso de uso para consultar los usuarios registrados
 * dentro del sistema.
 *
 * <p>
 * Este contrato abstrae la operación de consulta de usuarios,
 * permitiendo que la capa de aplicación exponga únicamente la
 * información necesaria mediante DTOs de respuesta.
 * </p>
 *
 * @author DarwinG
 */
public interface ListUsersUseCase {

    /**
     * Obtiene la lista de usuarios registrados.
     *
     * @return lista de usuarios con información pública del sistema
     */
    List<UserResponse> findAll();
}
