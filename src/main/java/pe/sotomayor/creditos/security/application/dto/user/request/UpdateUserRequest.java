package pe.sotomayor.creditos.security.application.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para actualizar la información de un usuario existente.
 *
 * <p>
 * Contiene los datos administrativos modificables de un usuario dentro
 * del sistema.
 * </p>
 *
 * @param fullName nombre completo del usuario
 * @param email    correo electrónico del usuario
 * @param roleId   identificador del nuevo rol asignado
 * @param branchId identificador de la nueva sucursal asignada
 * @author DarwinG
 */
public record UpdateUserRequest(

        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(
                max = 150,
                message = "El nombre completo no puede superar los 150 caracteres."
        )
        String fullName,


        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico debe tener un formato válido.")
        @Size(
                max = 120,
                message = "El correo electrónico no puede superar los 120 caracteres."
        )
        String email,


        @NotNull(message = "El rol es obligatorio.")
        Long roleId,


        @NotNull(message = "La sucursal es obligatoria.")
        Long branchId

) {
}
