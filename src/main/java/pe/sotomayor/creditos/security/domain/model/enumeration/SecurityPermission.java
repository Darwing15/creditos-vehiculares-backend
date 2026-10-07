package pe.sotomayor.creditos.security.domain.model.enumeration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Define las autoridades estables admitidas por el modulo de seguridad.
 *
 * <p>
 * Un rol almacena estos codigos, independientemente de su nombre visible.
 * El catalogo permite validar solicitudes y calcular los permisos necesarios
 * para conservar un administrador capaz de gestionar todo el modulo.
 * </p>
 *
 * @author DarwinG
 */
public enum SecurityPermission {
    /**
     * Permite consultar los usuarios de la administración central.
     */
    USERS_READ("security:users:read"),
    /**
     * Permite crear, editar o desactivar usuarios, sujeto a las reglas del servicio.
     */
    USERS_WRITE("security:users:write"),
    /**
     * Permite asignar un rol; se exige además de la escritura de usuarios.
     */
    USERS_ASSIGN_ROLE("security:users:assign-role"),
    /**
     * Permite consultar roles, sus permisos y el catálogo de autoridades.
     */
    ROLES_READ("security:roles:read"),
    /**
     * Permite gestionar metadatos y desactivación de roles, sin conceder permisos.
     */
    ROLES_WRITE("security:roles:write"),
    /**
     * Permite sustituir los permisos de un rol dentro de los privilegios del actor.
     */
    ROLES_MANAGE_PERMISSIONS("security:roles:manage-permissions"),
    /**
     * Permite consultar las sucursales del módulo administrativo.
     */
    BRANCHES_READ("security:branches:read"),
    /**
     * Permite crear, editar y desactivar sucursales bajo las reglas de negocio.
     */
    BRANCHES_WRITE("security:branches:write"),

    CUSTOMERS_READ("customers:read"),

    /**
     * Crear y editar fichas de la sucursal.
     */
    CUSTOMERS_WRITE("customers:write"),

    VEHICLES_READ("vehicles:read"),

    /**
     * Crear y editar unidades disponibles de la sucursal.
     */
    VEHICLES_WRITE("vehicles:write"),

    /**
     * Consulta solicitudes de la sucursal autenticada.
     */
    CREDIT_APPLICATIONS_READ("credit-applications:read"),
    /**
     * Simula y registra solicitudes sin resolverlas.
     */
    CREDIT_APPLICATIONS_CREATE("credit-applications:create"),
    /**
     * Aprueba, rechaza o cancela solicitudes de su sucursal.
     */
    CREDIT_APPLICATIONS_RESOLVE("credit-applications:resolve");

    /**
     * Código persistido y comparado literalmente al evaluar una autorización.
     */
    private final String authority;

    /**
     * Asocia una constante Java con su código de autorización estable.
     *
     * @param authority código definido por el catálogo del módulo
     */
    SecurityPermission(String authority) {
        this.authority = authority;
    }

    /**
     * Obtiene el código utilizado en persistencia, respuestas y Spring Security.
     *
     * @return autoridad exacta de este permiso
     */
    public String authority() {
        return authority;
    }

    /**
     * Obtiene una colección inmutable con todas las autoridades admitidas.
     *
     * @return catálogo completo sin duplicados
     */
    public static Set<String> authorities() {
        return Arrays.stream(values())
                .map(SecurityPermission::authority)
                .collect(Collectors.toUnmodifiableSet());
    }
}