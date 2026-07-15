package pe.sotomayor.creditos.security.application.usecase.auth;

import pe.sotomayor.creditos.security.application.dto.auth.request.LoginRequest;
import pe.sotomayor.creditos.security.application.dto.auth.response.LoginResponse;

/**
 * Define el caso de uso para autenticar usuarios dentro del sistema.
 *
 * <p>
 * Este contrato abstrae la operación de validación de credenciales
 * y generación de la identidad autenticada del usuario.
 * </p>
 *
 * <p>
 * La implementación concreta es responsable de coordinar las validaciones
 * necesarias y delegar la generación del token a la infraestructura
 * correspondiente.
 * </p>
 *
 * @author DarwinG
 */
public interface AuthenticateUserUseCase {

    /**
     * Autentica un usuario mediante sus credenciales.
     *
     * @param request credenciales proporcionadas por el usuario
     * @return información del usuario autenticado junto con su token
     */
    LoginResponse authenticate(LoginRequest request);

}