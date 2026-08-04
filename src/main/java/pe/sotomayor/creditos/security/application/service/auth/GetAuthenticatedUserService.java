package pe.sotomayor.creditos.security.application.service.auth;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AnonymousAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

import pe.sotomayor.creditos.security.application.dto.auth.response.AuthenticatedUserResponse;

import pe.sotomayor.creditos.security.application.usecase.auth.GetAuthenticatedUserUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.User;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.UnauthorizedException;

/**
 * Implementa el caso de uso encargado de obtener la información
 * del usuario autenticado.
 *
 * <p>
 * Recupera la autenticación almacenada por Spring Security, obtiene
 * la información actual del usuario desde la base de datos y construye
 * la respuesta utilizada por los clientes de la aplicación.
 * </p>
 *
 * <p>
 * La información retornada siempre proviene de la base de datos,
 * garantizando que refleje el estado más reciente del usuario,
 * independientemente del contenido del token JWT.
 * </p>
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
public class GetAuthenticatedUserService implements GetAuthenticatedUserUseCase {

    private final UserRepository userRepository;

    /**
     * Obtiene la información del usuario autenticado.
     *
     * @return información del usuario autenticado
     */
    @Override
    public AuthenticatedUserResponse getCurrentUser() {

        Authentication authentication = getAuthentication();

        User user = findAuthenticatedUser(authentication.getName());

        return buildResponse(user);
    }

    /**
     * Obtiene la autenticación almacenada por Spring Security.
     *
     * <p>
     * Verifica que exista un usuario autenticado y que la autenticación
     * no corresponda a un usuario anónimo.
     * </p>
     *
     * @return autenticación válida
     * @throws UnauthorizedException si no existe un usuario autenticado
     */
    private Authentication getAuthentication() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new UnauthorizedException(
                    ErrorCode.UNAUTHORIZED,
                    "User is not authenticated.");
        }

        return authentication;
    }

    /**
     * Obtiene el usuario autenticado desde la base de datos.
     *
     * @param email correo electrónico autenticado
     * @return usuario encontrado
     * @throws ResourceNotFoundException si el usuario no existe
     */
    private User findAuthenticatedUser(String email) {

        return userRepository
                .findUserWithRoleAndBranchByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.USER_NOT_FOUND,
                                "User not found."));
    }

    /**
     * Construye la respuesta con la información del usuario autenticado.
     *
     * @param user usuario autenticado
     * @return respuesta enviada al cliente
     */
    private AuthenticatedUserResponse buildResponse(User user) {

        return new AuthenticatedUserResponse(

                user.getId(),

                user.getFullName(),

                user.getEmail(),

                user.getRole().getId(),

                user.getRole().getName(),

                user.getBranch().getId(),

                user.getBranch().getName()
        );
    }

}