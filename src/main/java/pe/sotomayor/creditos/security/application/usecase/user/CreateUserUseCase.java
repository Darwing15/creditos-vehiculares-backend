package pe.sotomayor.creditos.security.application.usecase.user;

import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;
import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;

/**
 * Define el caso de uso para registrar nuevos usuarios.
 *
 * <p>
 * Este contrato abstrae la operación de creación de usuarios dentro
 * del módulo de seguridad.
 * </p>
 *
 * @author DarwinG
 */
public interface CreateUserUseCase {

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param request información requerida para crear el usuario
     * @return información del usuario creado
     */
    UserResponse create(CreateUserRequest request);

}
