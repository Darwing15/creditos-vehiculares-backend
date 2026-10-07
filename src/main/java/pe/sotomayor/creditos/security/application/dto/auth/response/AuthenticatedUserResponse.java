package pe.sotomayor.creditos.security.application.dto.auth.response;

import java.util.List;

/**
 * Expone datos visibles y permisos del principal cargado para la petición actual.
 *
 * <p>No incluye credenciales ni entidades JPA. Los permisos sirven a la UI,
 * mientras que el servidor vuelve a autorizar cada operación.</p>
 *
 * @param id          identidad estable; se conserva el nombre actual del contrato /me
 * @param fullName    nombre visible del usuario
 * @param email       correo informativo
 * @param roleId      identificador del rol
 * @param roleName    nombre visible del rol
 * @param branchId    identificador de la sucursal asignada
 * @param branchName  nombre visible de sucursal
 * @param permissions códigos de autoridad para rutas, menús y acciones de la UI
 * @author DarwinG
 */
public record AuthenticatedUserResponse(

        Long id,

        String fullName,

        String email,

        Long roleId,

        String roleName,

        Long branchId,

        String branchName,

        List<String> permissions

) {
}
