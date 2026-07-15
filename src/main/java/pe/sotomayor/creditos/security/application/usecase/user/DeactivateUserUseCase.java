package pe.sotomayor.creditos.security.application.usecase.user;

import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

/**
 * Define el caso de uso para desactivar usuarios existentes.
 *
 * <p>
 * Este contrato abstrae la operación de cambio de estado funcional
 * de usuarios dentro del módulo de seguridad.
 * </p>
 *
 * @author DarwinG
 */
public interface DeactivateUserUseCase {

    /**
     * Desactiva un usuario existente.
     *
     * @param id identificador único del usuario
     * @return información del usuario desactivado
     */
    UserResponse deactivate(Long id);
}
