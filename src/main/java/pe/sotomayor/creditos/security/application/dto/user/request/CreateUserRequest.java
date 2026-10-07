package pe.sotomayor.creditos.security.application.dto.user.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

/**
 * Solicitud para registrar un nuevo usuario en el sistema.
 *
 * <p>
 * Contiene la información mínima requerida para crear un usuario asociado a un
 * rol y una sucursal.
 * </p>
 *
 * @param fullName nombre completo del usuario
 * @param email    correo electrónico utilizado para autenticación
 * @param password contraseña inicial del usuario
 * @param roleId   identificador del rol asignado
 * @param branchId identificador de la sucursal asignada
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
                min = 15,
                max = 72,
                message = "La contraseña debe tener entre 15 y 72 caracteres."
        )
        String password,

        @NotNull(message = "El rol es obligatorio.")
        Long roleId,

        @NotNull(message = "La sucursal es obligatoria.")
        Long branchId

) {
    /**
     * Complementa la longitud en caracteres del alta con el límite en bytes de BCrypt.
     *
     * <p>El nombre is... permite que Bean Validation lo evalúe como propiedad.
     * JsonIgnore impide incorporarlo al contrato JSON. NotBlank controla el valor nulo.</p>
     *
     * @return true si el valor es nulo o cabe en 72 bytes; NotBlank trata la ausencia
     */
    @JsonIgnore
    @AssertTrue(
            message = "La contraseña no puede superar 72 bytes en UTF-8.")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null
                || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
