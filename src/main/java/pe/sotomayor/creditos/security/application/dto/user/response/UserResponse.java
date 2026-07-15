package pe.sotomayor.creditos.security.application.dto.user.response;

/**
 * Información de un usuario del sistema.
 *
 * <p>
 * Representa los datos públicos de un usuario utilizados por los casos
 * de uso del módulo de seguridad.
 * </p>
 *
 * @param id         identificador único del usuario
 * @param fullName   nombre completo del usuario
 * @param email      correo electrónico utilizado para autenticación
 * @param roleId     identificador del rol asignado
 * @param roleName   nombre del rol asignado
 * @param branchId   identificador de la sucursal asignada
 * @param branchName nombre de la sucursal asignada
 * @param active     indica si el usuario está habilitado para operar dentro del sistema
 * @author DarwinG
 */
public record UserResponse(

        Long id,

        String fullName,

        String email,

        Long roleId,

        String roleName,

        Long branchId,

        String branchName,

        Boolean active

) {
}
