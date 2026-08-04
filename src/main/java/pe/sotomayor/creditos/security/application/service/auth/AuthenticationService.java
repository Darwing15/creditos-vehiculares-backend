package pe.sotomayor.creditos.security.application.service.auth;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.auth.request.LoginRequest;
import pe.sotomayor.creditos.security.application.dto.auth.response.LoginResponse;

import pe.sotomayor.creditos.security.application.usecase.auth.AuthenticateUserUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.User;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

/**
 * Implementa el caso de uso encargado de autenticar usuarios.
 *
 * <p>
 * Delega la validación de credenciales al {@link AuthenticationManager}
 * de Spring Security. Una vez autenticado el usuario, recupera la
 * información completa desde la base de datos y genera el token JWT
 * que permitirá acceder a los recursos protegidos del sistema.
 * </p>
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticationService implements AuthenticateUserUseCase {

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    private final JwtService jwtService;

    /**
     * Autentica un usuario y genera un token JWT.
     *
     * @param request credenciales de acceso
     * @return información del usuario autenticado
     */
    @Override
    public LoginResponse authenticate(LoginRequest request) {

        Authentication authentication = authenticateUser(request);

        User user = findAuthenticatedUser(authentication.getName());

        String token = jwtService.generateToken(user);

        return buildResponse(user, token);
    }

    /**
     * Autentica las credenciales utilizando Spring Security.
     *
     * @param request credenciales proporcionadas por el usuario
     * @return autenticación validada
     */
    private Authentication authenticateUser(LoginRequest request) {

        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                ));
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
     * Construye la respuesta de autenticación.
     *
     * @param user  usuario autenticado
     * @param token token JWT generado
     * @return respuesta enviada al cliente
     */
    private LoginResponse buildResponse(
            User user,
            String token) {

        return new LoginResponse(
                token,
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