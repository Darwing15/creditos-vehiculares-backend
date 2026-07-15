package pe.sotomayor.creditos.security.application.dto.branch.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para actualizar la información de una sucursal existente.
 *
 * <p>
 * Contiene los datos administrativos modificables de una sucursal.
 * El código de sucursal no forma parte de esta solicitud debido a que
 * representa la identidad operativa del registro.
 * </p>
 *
 * @param name      nombre descriptivo de la sucursal
 * @param address   dirección física de la sucursal
 * @param phone     teléfono de contacto
 * @param email     correo electrónico de contacto
 * @param managerId identificador del usuario responsable de la sucursal
 * @author DarwinG
 */
public record UpdateBranchRequest(

        @NotBlank(message = "El nombre de la sucursal es obligatorio.")
        @Size(
                max = 100,
                message = "El nombre no puede superar los 100 caracteres."
        )
        String name,

        @NotBlank(message = "La dirección es obligatoria.")
        @Size(
                max = 255,
                message = "La dirección no puede superar los 255 caracteres."
        )
        String address,

        @Size(
                max = 20,
                message = "El teléfono no puede superar los 20 caracteres."
        )
        String phone,

        @Email(message = "El correo electrónico debe tener un formato válido.")
        @Size(
                max = 120,
                message = "El correo electrónico no puede superar los 120 caracteres."
        )
        String email,

        Long managerId

) {
}
