package pe.sotomayor.creditos.security.application.dto.user.response;

import java.util.List;

/**
 * Opciones presentes en los usuarios no eliminados del listado global.
 *
 * @param roles    roles asociados a usuarios
 * @param branches sucursales asociadas a usuarios
 */
public record UserFilterOptions(
        List<UserFilterOption> roles,
        List<UserFilterOption> branches) {
}
