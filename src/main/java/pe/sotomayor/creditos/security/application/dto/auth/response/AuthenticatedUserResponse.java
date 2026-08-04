package pe.sotomayor.creditos.security.application.dto.auth.response;

/**
 * Información del usuario autenticado.
 *
 * <p>
 * Se utiliza para exponer los datos básicos del usuario que posee
 * el token JWT actualmente válido.
 * </p>
 *
 * @author DarwinG
 */
public record AuthenticatedUserResponse(

        Long id,

        String fullName,

        String email,

        Long roleId,

        String roleName,

        Long branchId,

        String branchName

) {
}
