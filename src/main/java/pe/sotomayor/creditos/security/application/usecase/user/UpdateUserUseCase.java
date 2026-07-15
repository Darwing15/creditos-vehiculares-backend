package pe.sotomayor.creditos.security.application.usecase.user;

import pe.sotomayor.creditos.security.application.dto.user.request.UpdateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

/**
 * Define el caso de uso para actualizar usuarios existentes.
 *
 * <p>
 * Este contrato abstrae la operación de modificación de información
 * administrativa de usuarios dentro del módulo de seguridad.
 * </p>
 *
 * @author DarwinG
 */
public interface UpdateUserUseCase {

    /**
     * Actualiza la información de un usuario existente.
     *
     * @param id      identificador único del usuario
     * @param request información actualizada del usuario
     * @return información del usuario actualizado
     */
    UserResponse update(Long id, UpdateUserRequest request);

}
