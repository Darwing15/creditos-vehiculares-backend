package pe.sotomayor.creditos.security.application.dto.role.response;

/**
 * Información de un rol del sistema.
 *
 * <p>
 * Representa los datos de un rol devueltos por los casos de uso del módulo
 * de seguridad.
 * </p>
 *
 * @param id          identificador único del rol
 * @param name        nombre del rol
 * @param description descripción funcional del rol
 *
 * @author DarwinG
 */
public record RoleResponse(

        Long id,

        String name,

        String description

) {
}
