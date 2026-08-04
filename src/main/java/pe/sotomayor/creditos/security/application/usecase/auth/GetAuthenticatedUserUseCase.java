package pe.sotomayor.creditos.security.application.usecase.auth;

import pe.sotomayor.creditos.security.application.dto.auth.response.AuthenticatedUserResponse;

/**
 * Obtiene la información del usuario autenticado.
 *
 * <p>
 * Permite recuperar los datos básicos del usuario asociado al
 * token JWT utilizado en la solicitud actual.
 * </p>
 *
 * <p>
 * Este caso de uso es utilizado por el endpoint
 * <strong>GET /api/auth/me</strong>, permitiendo que el cliente
 * conozca la identidad del usuario autenticado sin necesidad de
 * reenviar sus credenciales.
 * </p>
 *
 * @author DarwinG
 */
public interface GetAuthenticatedUserUseCase {

    /**
     * Obtiene la información del usuario autenticado.
     *
     * @return información del usuario autenticado
     */
    AuthenticatedUserResponse getCurrentUser();

}
