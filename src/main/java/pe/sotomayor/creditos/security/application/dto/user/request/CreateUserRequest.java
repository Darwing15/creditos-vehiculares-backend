package pe.sotomayor.creditos.security.application.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para registrar un nuevo usuario en el sistema.
 *
 * <p>
 * Contiene la información mínima requerida para crear un usuario asociado a un
 * rol y una sucursal.
 * </p>
 *
 * @param fullName nombre completo del usuario
 * @param email correo electrónico utilizado para autenticación
 * @param password contraseña inicial del usuario
 * @param roleId identificador del rol asignado
 * @param branchId identificador de la sucursal asignada
 *
 * @author DarwinG
 */
public record CreateUserRequest(

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

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(
                min = 8,
                max = 100,
                message = "La contraseña debe tener entre 8 y 100 caracteres."
        )
        String password,

        @NotNull(message = "El rol es obligatorio.")
        Long roleId,

        @NotNull(message = "La sucursal es obligatoria.")
        Long branchId

) {
}
