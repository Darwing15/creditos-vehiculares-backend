package pe.sotomayor.creditos.security.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import pe.sotomayor.creditos.security.application.dto.auth.request.ChangePasswordRequest;
import pe.sotomayor.creditos.security.application.dto.auth.request.LoginRequest;
import pe.sotomayor.creditos.security.application.dto.auth.response.AuthenticatedUserResponse;
import pe.sotomayor.creditos.security.application.dto.auth.response.LoginResponse;

import pe.sotomayor.creditos.security.application.usecase.auth.AuthenticateUserUseCase;
import pe.sotomayor.creditos.security.application.usecase.auth.ChangePasswordUseCase;
import pe.sotomayor.creditos.security.application.usecase.auth.GetAuthenticatedUserUseCase;
import pe.sotomayor.creditos.security.application.usecase.auth.LogoutAllUseCase;

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

    private final GetAuthenticatedUserUseCase getAuthenticatedUserUseCase;

    private final ChangePasswordUseCase changePasswordUseCase;

    private final LogoutAllUseCase logoutAllUseCase;

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

    /**
     * Obtiene la información del usuario autenticado.
     *
     * <p>
     * Recupera los datos actuales del usuario cuya identidad fue validada
     * mediante el token JWT enviado en la cabecera Authorization.
     * </p>
     *
     * <p>
     * La información se consulta directamente desde la base de datos,
     * garantizando que siempre refleje el estado más reciente del usuario,
     * incluso si el token fue emitido anteriormente.
     * </p>
     *
     * @return información del usuario autenticado
     */
    @GetMapping("/me")
    @Operation(
            summary = "Obtiene el usuario autenticado",
            description = """
                    Devuelve la información del usuario que realizó
                    la autenticación utilizando el token JWT enviado
                    en la cabecera Authorization.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario autenticado obtenido correctamente"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token inválido o no enviado"
            )
    })
    public ResponseEntity<AuthenticatedUserResponse> me() {

        return ResponseEntity.ok(
                getAuthenticatedUserUseCase.getCurrentUser()
        );
    }

    /**
     * Actualiza la contraseña del usuario autenticado.
     *
     * <p>
     * Requiere que el usuario proporcione su contraseña actual junto con
     * la nueva contraseña que desea establecer. La identidad del usuario
     * se obtiene automáticamente a partir del token JWT enviado en la
     * solicitud.
     * </p>
     *
     * <p>
     * Si el proceso finaliza correctamente, la operación responde con
     * el código HTTP {@code 204 No Content}.
     * </p>
     *
     * @param request datos necesarios para realizar el cambio de contraseña
     */
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid
            @RequestBody
            ChangePasswordRequest request) {

        changePasswordUseCase.changePassword(request);
    }

    /**
     * Expone el cierre global de sesiones de la cuenta autenticada.
     *
     * <p>La cadena exige autenticación; tras completar el caso de uso responde 204.
     * No recibe un ID de actor y no implementa cierre de un único dispositivo.</p>
     */
    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll() {
        logoutAllUseCase.logoutAll();
    }

}
