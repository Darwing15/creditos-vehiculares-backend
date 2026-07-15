package pe.sotomayor.creditos.security.application.dto.auth.response;

/**
 * Respuesta generada después de autenticar correctamente
 * un usuario dentro del sistema.
 *
 * <p>
 * Contiene la información necesaria para que el cliente
 * pueda establecer la sesión del usuario autenticado.
 * </p>
 *
 * <p>
 * El token corresponde a la credencial temporal utilizada
 * para acceder a recursos protegidos de la aplicación.
 * </p>
 *
 * @param token      token de autenticación generado para el usuario
 * @param userId     identificador único del usuario autenticado
 * @param fullName   nombre completo del usuario
 * @param email      correo electrónico utilizado para autenticación
 * @param roleId     identificador del rol asignado
 * @param roleName   nombre del rol asignado
 * @param branchId   identificador de la sucursal asignada
 * @param branchName nombre de la sucursal asignada
 * @author DarwinG
 */
public record LoginResponse(

        String token,

        Long userId,

        String fullName,

        String email,

        Long roleId,

        String roleName,

        Long branchId,

        String branchName

) {
}
