package pe.sotomayor.creditos.security.application.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud utilizada para actualizar la contraseña del usuario
 * autenticado.
 *
 * <p>
 * Contiene la contraseña actual del usuario y la nueva contraseña
 * que desea establecer. La identidad del usuario no forma parte de
 * esta solicitud, ya que se obtiene a partir del contexto de
 * autenticación administrado por Spring Security.
 * </p>
 *
 * @param currentPassword contraseña actual
 * @param newPassword     nueva contraseña
 * @author DarwinG
 */
public record ChangePasswordRequest(

        @NotBlank(message = "La contraseña actual es obligatoria.")
        String currentPassword,

        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(
                min = 8,
                max = 100,
                message = "La nueva contraseña debe tener entre 8 y 100 caracteres."
        )
        String newPassword

) {
}
