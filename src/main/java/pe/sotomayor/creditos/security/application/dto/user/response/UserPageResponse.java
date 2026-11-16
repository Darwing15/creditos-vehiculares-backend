package pe.sotomayor.creditos.security.application.dto.user.response;

import java.util.List;

/**
 * Resultado de una busqueda paginada de usuarios.
 *
 * @param items         usuarios de la pagina
 * @param page          indice de pagina desde cero
 * @param size          tamaño solicitado
 * @param totalElements numero de usuarios que cumplen el filtro
 * @param totalPages    numero de paginas del resultado
 */
public record UserPageResponse(
        List<UserResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
