package pe.sotomayor.creditos.security.application.dto.user.response;

/**
 * Opcion visible de filtro de usuarios.
 *
 * @param id   identificador del rol o de la sucursal
 * @param name nombre mostrado
 */
public record UserFilterOption(
        Long id,
        String name) {
}
