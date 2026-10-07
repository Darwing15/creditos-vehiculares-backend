package pe.sotomayor.creditos.security.infrastructure.security.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

/**
 * Adaptador del principal de seguridad al puerto de sucursal de los modulos operativos.
 *
 * @author DarwinG
 */
@Component
@RequiredArgsConstructor
public class SecurityCurrentBranchProvider implements CurrentBranchProvider {
    private final CurrentSecurityPrincipal current;

    /**
     * @return la sucursal cargada en el principal validado.
     */
    @Override
    public Long getCurrentBranchId() {
        return current.get().getBranchId();
    }
}
