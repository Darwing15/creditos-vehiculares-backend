package pe.sotomayor.creditos.security.application.service.auth;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.auth.request.LoginRequest;
import pe.sotomayor.creditos.security.application.dto.auth.response.LoginResponse;

import pe.sotomayor.creditos.security.application.usecase.auth.AuthenticateUserUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.User;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.UnauthorizedException;

/**
 * Implementa el caso de uso para autenticar usuarios.
 *
 * <p>
 * Coordina la validación de credenciales de acceso y la construcción
 * de la respuesta de autenticación.
 * </p>
 *
 * <p>
 * La generación del token de autenticación es delegada a la
 * infraestructura de seguridad.
 * </p>
 *
 * @author DarwinG
 */
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AuthenticationService implements AuthenticateUserUseCase {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * Autentica un usuario mediante correo electrónico y contraseña.
     *
     * @param request credenciales del usuario
     * @return información del usuario autenticado
     */
    @Override
    public LoginResponse authenticate(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(this::invalidCredentials);

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword())) {

            throw invalidCredentials();
        }

        /*
         * TODO:
         * Reemplazar por JwtService.generateToken(user)
         * cuando se implemente la infraestructura JWT.
         */
        String token = "";

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

    /**
     * Construye la excepción utilizada cuando las credenciales
     * proporcionadas no son válidas.
     *
     * @return excepción de autenticación
     */
    private UnauthorizedException invalidCredentials() {

        return new UnauthorizedException(
                ErrorCode.INVALID_CREDENTIALS,
                "Invalid credentials."
        );
    }

}