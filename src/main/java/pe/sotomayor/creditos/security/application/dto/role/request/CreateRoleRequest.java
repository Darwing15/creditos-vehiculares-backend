package pe.sotomayor.creditos.security.application.dto.role.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para registrar un nuevo rol en el sistema.
 *
 * <p>
 * Contiene la información mínima requerida para crear un rol dentro del
 * módulo de seguridad.
 * </p>
 *
 * @param name        nombre único del rol
 * @param description descripción funcional del rol
 * @author DarwinG
 */
public record CreateRoleRequest(

        @NotBlank(message = "El nombre del rol es obligatorio.")
        @Size(max = 100, message = "El nombre del rol no puede superar los 100 caracteres.")
        String name,

        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres.")
        String description

) {
}
