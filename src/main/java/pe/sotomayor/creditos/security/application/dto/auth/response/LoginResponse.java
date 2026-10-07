package pe.sotomayor.creditos.security.application.dto.auth.response;

/**
 * Respuesta pública de un inicio de sesión correcto.
 *
 * <p>Conserva los campos existentes y agrega tipo/duración para que el cliente
 * gestione una sesión temporal sin analizar claims para tomar decisiones de acceso.</p>
 *
 * @param token      access token firmado; no registrar ni persistir en logs
 * @param userId     identidad estable, conservando el nombre de campo del contrato actual
 * @param fullName   nombre visible del usuario
 * @param email      correo informativo de la cuenta autenticada
 * @param roleId     identificador del rol asignado
 * @param roleName   nombre visible del rol, sin significado de autorización
 * @param branchId   identificador de la sucursal asignada
 * @param branchName nombre visible de sucursal
 * @param tokenType  esquema de envío de la credencial; en esta propuesta, Bearer
 * @param expiresIn  duración nominal en segundos, no en milisegundos
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

        String branchName,

        String tokenType,

        long expiresIn

) {
}
