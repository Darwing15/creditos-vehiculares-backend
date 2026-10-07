package pe.sotomayor.creditos.security.application.service.auth;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.GrantedAuthority;

import org.springframework.stereotype.Service;

import pe.sotomayor.creditos.security.application.dto.auth.response.AuthenticatedUserResponse;

import pe.sotomayor.creditos.security.application.usecase.auth.GetAuthenticatedUserUseCase;

import pe.sotomayor.creditos.security.infrastructure.security.service.CurrentSecurityPrincipal;

/**
 * Construye /me desde el principal cargado y validado por el filtro de esta petición.
 *
 * <p>Reemplaza la búsqueda adicional por correo y expone permisos explícitos.</p>
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
public class GetAuthenticatedUserService implements GetAuthenticatedUserUseCase {

    /**
     * Proveedor tipado del principal actual; evita acceder directamente al contexto aquí.
     */
    private final CurrentSecurityPrincipal current;

    /**
     * Obtiene la vista pública del usuario y sus códigos de permiso.
     *
     * @return identidad y permisos disponibles en el principal de la solicitud
     * @throws org.springframework.security.authentication.BadCredentialsException si falta el principal autenticado esperado
     */
    @Override
    public AuthenticatedUserResponse getCurrentUser() {
        /* Instantánea cargada por el filtro, reutilizada sin consultar por correo. */
        var p = current.get();

        return new AuthenticatedUserResponse(
                p.getUserId(), p.getFullName(), p.getUsername(),
                p.getRoleId(), p.getRoleName(), p.getBranchId(), p.getBranchName(),
                p.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .sorted().toList());
    }

}