package pe.sotomayor.creditos.security.presentation.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.sotomayor.creditos.security.application.dto.auth.request.LoginRequest;
import pe.sotomayor.creditos.security.application.dto.auth.response.LoginResponse;

import pe.sotomayor.creditos.security.application.usecase.auth.AuthenticateUserUseCase;

/**
 * Expone los endpoints públicos de autenticación del sistema.
 *
 * <p>
 * Permite que los usuarios registrados inicien sesión mediante sus
 * credenciales y obtengan un token JWT para acceder a los recursos
 * protegidos de la aplicación.
 * </p>
 *
 * <p>
 * Este controlador delega completamente la lógica de autenticación
 * al caso de uso correspondiente, manteniendo la capa de presentación
 * libre de reglas de negocio.
 * </p>
 *
 * @author DarwinG
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;

    /**
     * Autentica un usuario y genera un token JWT.
     *
     * @param request credenciales de autenticación
     * @return token JWT e información básica del usuario autenticado
     */
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid
                               @RequestBody
                               LoginRequest request) {

        return authenticateUserUseCase.authenticate(request);
    }

}
