package pe.sotomayor.creditos.security.application.dto.auth.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

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
        @Size(max = 256)
        String currentPassword,

        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(
                min = 15,
                max = 72,
                message = "La nueva contraseña debe tener entre 15 y 72 caracteres."
        )
        String newPassword

) {
    /**
     * Comprueba el límite UTF-8 de la nueva contraseña antes de codificarla.
     *
     * <p>La contraseña actual sirve para verificar un hash existente; la política
     * de alta corresponde a newPassword. NotBlank controla la ausencia del valor.</p>
     *
     * @return true si newPassword es nula o no supera 72 bytes UTF-8
     */
    @JsonIgnore
    @AssertTrue(
            message = "La contraseña no puede superar 72 bytes en UTF-8.")
    public boolean isNewPasswordWithinBcryptLimit() {
        return newPassword == null
                || newPassword.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
