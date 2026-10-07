package pe.sotomayor.creditos.security.application.service.auth;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.stereotype.Service;

import pe.sotomayor.creditos.security.application.dto.auth.request.LoginRequest;
import pe.sotomayor.creditos.security.application.dto.auth.response.LoginResponse;

import pe.sotomayor.creditos.security.application.usecase.auth.AuthenticateUserUseCase;

import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;

import java.util.Locale;

/**
 * Coordina la comprobación de credenciales y la emisión de una sesión temporal.
 *
 * <p>Reemplaza la segunda lectura por correo: se firma con el principal cuyo
 * hash fue comprobado, conservando la misma identidad y versión observadas.</p>
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService implements AuthenticateUserUseCase {

    /**
     * Administrador que verifica contraseña y estado de la cuenta mediante el proveedor DAO.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Emisor de tokens; no comprueba por sí mismo la contraseña del usuario.
     */
    private final JwtService jwtService;

    /**
     * Autentica las credenciales y construye la respuesta con duración explícita.
     *
     * @param request credenciales validadas; la contraseña se conserva exactamente
     * @return credencial de acceso y datos visibles del mismo principal autenticado
     * @throws AuthenticationException si fallan credenciales,
     *                                 estado de cuenta o el proveedor de autenticación
     */
    @Override
    public LoginResponse authenticate(LoginRequest request) {

        /* Resultado del proveedor, no una identidad declarada por el cliente. */
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.email().strip().toLowerCase(Locale.ROOT),
                        request.password()));

        /* p es el principal que superó las comprobaciones de ese proveedor. */
        var p = (SecurityPrincipal) authentication.getPrincipal();

        return new LoginResponse(jwtService.generateToken(p),
                p.getUserId(), p.getFullName(), p.getUsername(),
                p.getRoleId(), p.getRoleName(), p.getBranchId(), p.getBranchName(),
                "Bearer", jwtService.expiresInSeconds());
    }

}