package pe.sotomayor.creditos.security.application.usecase.user;

import pe.sotomayor.creditos.security.application.dto.user.response.UserFilterOptions;
import pe.sotomayor.creditos.security.application.dto.user.response.UserPageResponse;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.util.List;

/**
 * Define el caso de uso para consultar los usuarios registrados
 * dentro del sistema.
 *
 * <p>
 * Este contrato abstrae la operacion de consulta de usuarios,
 * permitiendo que la capa de aplicacion exponga unicamente la
 * informacion necesaria mediante DTOs de respuesta.
 * </p>
 *
 * @author DarwinG
 */
public interface ListUsersUseCase {

    /**
     * Obtiene la lista de usuarios registrados.
     *
     * @return lista de usuarios con informacion publica del sistema
     */
    List<UserResponse> findAll();

    /**
     * Busca usuarios no eliminados de todas las sucursales, incluidos los inactivos.
     *
     * @param query    fragmento literal de nombre, correo o rol; nulo equivale a vacio
     * @param roleId   filtro opcional por identificador positivo de rol
     * @param branchId filtro opcional por identificador positivo de sucursal
     * @param page     pagina desde cero, hasta 1 000 000
     * @param size     tamaño de pagina entre 1 y 100
     * @return pagina ordenada por identificador ascendente y sus totales
     * @throws ValidationException si los filtros o límites son invalidos
     */
    UserPageResponse searchPage(String query, Long roleId, Long branchId, int page, int size);

    /**
     * Obtiene roles y sucursales asociados a usuarios no eliminados.
     *
     * @return opciones globales para los filtros de usuarios
     */
    UserFilterOptions filterOptions();
}
