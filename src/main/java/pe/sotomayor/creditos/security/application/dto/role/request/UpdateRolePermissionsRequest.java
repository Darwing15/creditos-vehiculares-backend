package pe.sotomayor.creditos.security.application.dto.role.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Sustituye los permisos del rol. El servicio limita los valores al catálogo
 * SecurityPermission y a los privilegios efectivos del actor. El máximo de
 * 64 valores es un límite de entrada, independiente del tamaño del catálogo.
 *
 * @param permissions conjunto destino; vacío retira las concesiones
 * @author DarwinG
 */
public record UpdateRolePermissionsRequest(
        @NotNull
        @Size(max = 64)
        Set<@NotBlank String> permissions
) {
}
