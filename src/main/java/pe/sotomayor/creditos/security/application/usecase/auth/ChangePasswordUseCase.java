package pe.sotomayor.creditos.security.application.usecase.auth;

import pe.sotomayor.creditos.security.application.dto.auth.request.ChangePasswordRequest;

/**
 * Caso de uso encargado de actualizar la contraseña del usuario
 * autenticado.
 *
 * <p>
 * Valida la identidad del usuario, verifica la contraseña actual
 * y registra una nueva contraseña cifrada utilizando el mecanismo
 * definido por la infraestructura de seguridad.
 * </p>
 *
 * @author DarwinG
 */
public interface ChangePasswordUseCase {

    /**
     * Actualiza la contraseña del usuario autenticado.
     *
     * @param request datos necesarios para el cambio de contraseña
     */
    void changePassword(ChangePasswordRequest request);

}
