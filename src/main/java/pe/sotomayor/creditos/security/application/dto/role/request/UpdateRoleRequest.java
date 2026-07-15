package pe.sotomayor.creditos.security.application.dto.role.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos editables requeridos para actualizar un rol existente.
 *
 * <p>
 * El identificador del rol no forma parte del request porque pertenece al
 * contexto de la operación de actualización.
 * </p>
 * @param name nombre del rol
 * @param description descripción funcional del rol
 * @author DarwinG
 */
public record UpdateRoleRequest(

        @NotBlank(message = "El nombre del rol es obligatorio.")
        @Size(max = 100, message = "El nombre del rol no puede superar los 100 caracteres.")
        String name,

        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres.")
        String description

) {
}
