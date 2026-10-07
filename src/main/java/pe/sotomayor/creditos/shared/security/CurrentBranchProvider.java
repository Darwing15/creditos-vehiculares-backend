package pe.sotomayor.creditos.shared.security;

/**
 * Puerto que expone la sucursal autenticada sin acoplar los modulos a Spring Security.
 *
 * @author DarwinG
 */
public interface CurrentBranchProvider {
    /**
     * @return identificador de sucursal asignado al usuario autenticado.
     */
    Long getCurrentBranchId();
}
