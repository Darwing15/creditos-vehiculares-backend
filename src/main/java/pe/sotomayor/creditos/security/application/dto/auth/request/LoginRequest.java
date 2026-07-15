package pe.sotomayor.creditos.security.application.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para autenticar un usuario dentro del sistema.
 *
 * <p>
 * Contiene las credenciales necesarias para iniciar sesión.
 * La validación de existencia del usuario y la comprobación
 * de contraseña son responsabilidades de la capa Application.
 * </p>
 *
 * @param email    correo electrónico utilizado como identificador
 *                 de autenticación
 * @param password contraseña proporcionada por el usuario
 * @author DarwinG
 */
public record LoginRequest(

        /**
         * Correo electrónico del usuario.
         */
        @NotBlank(
                message = "El correo electrónico es obligatorio."
        )
        @Email(
                message = "El correo electrónico debe tener un formato válido."
        )
        @Size(
                max = 120,
                message = "El correo electrónico no puede superar los 120 caracteres."
        )
        String email,


        /**
         * Contraseña del usuario.
         */
        @NotBlank(
                message = "La contraseña es obligatoria."
        )
        @Size(
                min = 8,
                max = 100,
                message = "La contraseña debe tener entre 8 y 100 caracteres."
        )
        String password

) {
}
