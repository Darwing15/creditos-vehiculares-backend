package pe.sotomayor.creditos.security.infrastructure.security.model;

import lombok.Getter;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Representa la identidad y las autoridades cargadas para una autenticacion.
 *
 * <p>Sustituye el principal genérico para conservar ID y versión sin consultar
 * por un correo mutable. Los campos adicionales son finales, pero la instancia
 * no es completamente inmutable: hereda la credencial borrable de Spring User.</p>
 * <p>No es un DTO público ni una entidad JPA; no devolverlo directamente por HTTP.</p>
 *
 * @author DarwinG
 */
@Getter
public final class SecurityPrincipal
        extends User {

    /**
     * ID persistente del usuario; ancla la identidad aunque cambie su correo.
     */
    private final Long userId;

    /**
     * Versión de revocación observada al cargar el usuario, distinta de rowVersion.
     */
    private final Long tokenVersion;

    /**
     * Nombre visible para construir respuestas sin exponer la entidad de dominio.
     */
    private final String fullName;

    /**
     * ID del rol asignado en esta lectura; no concede autoridad por sí mismo.
     */
    private final Long roleId;

    /**
     * Nombre visible del rol; nunca se usa para deducir privilegios.
     */
    private final String roleName;

    /**
     * ID de sucursal de referencia; no establece automáticamente alcance de acceso.
     */
    private final Long branchId;

    /**
     * Nombre de sucursal utilizado únicamente para presentación.
     */
    private final String branchName;

    /**
     * Construye el principal a partir de datos ya cargados de la base de datos.
     *
     * @param userId       identificador estable del usuario
     * @param tokenVersion versión que se comparará con el claim tv
     * @param fullName     nombre visible
     * @param email        correo usado como username por compatibilidad
     * @param password     hash almacenado, nunca la contraseña en claro
     * @param roleId       identificador del rol
     * @param roleName     nombre informativo del rol
     * @param branchId     identificador de sucursal
     * @param branchName   nombre informativo de sucursal
     * @param enabled      resultado conjunto de estados de usuario, rol y sucursal
     * @param authorities  permisos explícitos de la base de datos
     */
    public SecurityPrincipal(
            Long userId, Long tokenVersion, String fullName,
            String email, String password, Long roleId, String roleName,
            Long branchId, String branchName, boolean enabled,
            Collection<? extends GrantedAuthority> authorities) {
        super(email, password, enabled, true, true, true, authorities);
        this.userId = userId;
        this.tokenVersion = tokenVersion;
        this.fullName = fullName;
        this.roleId = roleId;
        this.roleName = roleName;
        this.branchId = branchId;
        this.branchName = branchName;
    }
}
