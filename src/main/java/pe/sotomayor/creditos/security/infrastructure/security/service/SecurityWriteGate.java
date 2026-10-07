package pe.sotomayor.creditos.security.infrastructure.security.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.transaction.IllegalTransactionStateException;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.UnauthorizedException;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Coordina las escrituras administrativas que pueden cambiar el acceso al sistema.
 *
 * <p>El servicio invocador abre una transacción de escritura READ_COMMITTED,
 * llama a {@link #begin(SecurityPermission[])} antes de leer entidades y
 * finaliza sus modificaciones con {@link #finish()}. El bloqueo de la fila
 * común se mantiene hasta el commit o rollback de esa misma transacción.</p>
 *
 * <p>Lombok genera el constructor que recibe el EntityManager. La propagación
 * MANDATORY exige invocar este componente mediante el proxy de Spring desde
 * una transacción existente; no abre una transacción independiente.</p>
 *
 * @author DarwinG
 */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class SecurityWriteGate {

    /**
     * Ejecuta el bloqueo y las consultas escalares en la misma transacción que
     * las escrituras JPA; flush hace visibles los cambios al conteo final.
     */
    private final EntityManager entityManager;

    /**
     * Bloquea la coordinación administrativa y comprueba la sesión y los permisos
     * efectivos después de cualquier espera por otra transacción.
     *
     * @param required permisos que exige la operación que comienza
     * @return identidad y permisos inmutables del actor para validar esta escritura
     * @throws UnauthorizedException            si falta un principal válido, su cuenta o sus
     *                                          relaciones están indisponibles, o la versión de sesión ya no coincide
     * @throws AccessDeniedException            si falta alguno de los permisos requeridos
     * @throws IllegalStateException            si la transacción existente es de solo lectura
     * @throws IllegalTransactionStateException si la llamada a través del proxy no dispone de una transacción
     */
    public WriteContext begin(SecurityPermission... required) {
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException("Se requiere una transacción de escritura.");
        }

        /* auth identifica la sesión validada por Spring; principal aporta el ID
         estable y la versión JWT, sin aceptar un actor declarado por el cliente. */
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof SecurityPrincipal principal)) {
            throw unauthorized();
        }

        /* La clave singleton = 1 obliga a todos los escritores a esperar aquí. */
        entityManager.createNativeQuery("""
                SELECT singleton FROM security_admin_guard
                WHERE singleton = 1 FOR UPDATE
                """).getSingleResult();

        /* SQL escalar: leer estado actual después de esperar el bloqueo,
         evitando reutilizar entidades antiguas del contexto JPA.
         rows estará vacío si el usuario, su rol o su sucursal no son operativos. */
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT u.token_version
                        FROM users u
                        JOIN roles r ON r.id = u.role_id
                        JOIN branches b ON b.id = u.branch_id
                        WHERE u.id = :id
                          AND u.is_deleted = FALSE AND u.is_active = TRUE
                          AND r.is_deleted = FALSE
                          AND b.is_deleted = FALSE AND b.is_active = TRUE
                        """)
                .setParameter("id", principal.getUserId())
                .getResultList();

        /* Un cambio de tokenVersion durante la espera invalida también esta petición. */
        if (rows.isEmpty() || !Objects.equals(
                ((Number) rows.getFirst()).longValue(),
                principal.getTokenVersion())) {
            throw unauthorized();
        }

        /* stored lee las concesiones actuales; no reutiliza las authorities que
         el filtro pudo haber cargado antes de que otra petición revocara permisos. */
        List<?> stored = entityManager.createNativeQuery("""
                        SELECT rp.permission FROM role_permissions rp
                        JOIN users u ON u.role_id = rp.role_id
                        WHERE u.id = :id
                        """)
                .setParameter("id", principal.getUserId())
                .getResultList();

        /* permissions es el conjunto efectivo sobre el que se evalúa esta escritura. */
        Set<String> permissions = stored.stream()
                .map(String.class::cast)
                .collect(Collectors.toUnmodifiableSet());

        /* context se devuelve al servicio para comprobar permisos destino y actor. */
        WriteContext context = new WriteContext(principal.getUserId(), permissions);
        for (SecurityPermission permission : required) {
            context.require(permission);
        }
        return context;
    }

    /**
     * Sincroniza las modificaciones y verifica que quede un administrador activo.
     *
     * <p>Debe ejecutarse después de begin y de las escrituras, dentro de la misma
     * transacción. No realiza commit ni libera el bloqueo: si falla, la excepción
     * debe propagarse para revertir todas las modificaciones administrativas.</p>
     *
     * @throws ConflictException                si no queda ningún usuario operativo con el
     *                                          catálogo completo de permisos de seguridad
     * @throws IllegalTransactionStateException si la llamada a través del proxy no dispone de una transacción
     */
    public void finish() {
        entityManager.flush();

        /* Todos los permisos del catálogo actual definen al administrador
        capaz de recuperar la administración de seguridad. */
        List<String> required = SecurityPermission.authorities().stream()
                .sorted().toList();
        /* parameters contiene solo nombres de parámetros generados localmente;
        los códigos de permiso se enlazan por separado, nunca se concatenan al SQL. */
        String parameters = IntStream.range(0, required.size())
                .mapToObj(i -> ":p" + i)
                .collect(Collectors.joining(", "));

        /* query cuenta usuarios operativos cuyo rol contiene todos los permisos
         requeridos; la PK de role_permissions impide duplicados en ese conteo. */
        Query query = entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM users u
                JOIN roles r ON r.id = u.role_id
                JOIN branches b ON b.id = u.branch_id
                WHERE u.is_deleted = FALSE AND u.is_active = TRUE
                  AND r.is_deleted = FALSE
                  AND b.is_deleted = FALSE AND b.is_active = TRUE
                  AND (SELECT COUNT(*) FROM role_permissions rp
                       WHERE rp.role_id = r.id
                       AND rp.permission IN (%s)) = :requiredCount
                """.formatted(parameters));
        for (int i = 0; i < required.size(); i++) {
            query.setParameter("p" + i, required.get(i));
        }
        query.setParameter("requiredCount", required.size());

        if (((Number) query.getSingleResult()).longValue() == 0L) {
            throw new ConflictException(
                    ErrorCode.LAST_SECURITY_ADMIN_REQUIRED,
                    "Debe permanecer al menos un administrador de seguridad activo.");
        }
    }

    /**
     * Construye un error uniforme sin revelar qué condición invalidó la sesión.
     *
     * @return excepción de autenticación para propagar al manejador correspondiente
     */
    private UnauthorizedException unauthorized() {
        return new UnauthorizedException(
                ErrorCode.UNAUTHORIZED, "La sesión ya no es válida.");
    }

    /**
     * Conserva la identidad y los privilegios usados para una escritura administrativa.
     *
     * <p>Se obtiene de begin y se utiliza durante esa transacción; no representa
     * una sesión persistente ni debe almacenarse como caché de autorización.</p>
     *
     * @param userId      identificador del actor autenticado y revalidado
     * @param permissions autoridades efectivas leídas después de adquirir el bloqueo
     * @author DarwinG
     */
    public record WriteContext(Long userId, Set<String> permissions) {
        /**
         * Copia las autoridades para impedir cambios externos al contexto de escritura.
         *
         * @param userId      identificador del actor
         * @param permissions conjunto de autoridades que se conserva inmutable
         * @throws NullPointerException si el conjunto o alguno de sus elementos es nulo
         */
        public WriteContext {
            permissions = Set.copyOf(permissions);
        }

        /**
         * Exige una capacidad adicional durante la operación ya iniciada.
         *
         * @param permission permiso necesario, por ejemplo asignar un rol distinto
         * @throws AccessDeniedException si el actor no posee esa autoridad
         */
        public void require(SecurityPermission permission) {
            if (!permissions.contains(permission.authority())) {
                throw new AccessDeniedException("No tiene permiso para esta operación.");
            }
        }

        /**
         * Impide administrar o conceder capacidades que el actor no posee.
         *
         * @param targetPermissions permisos del recurso afectado o concesiones propuestas
         * @throws AccessDeniedException si alguno de los permisos supera los del actor
         */
        public void requireSubset(Set<String> targetPermissions) {
            if (!permissions.containsAll(targetPermissions)) {
                throw new AccessDeniedException(
                        "No puede administrar privilegios superiores a los propios.");
            }
        }
    }
}
