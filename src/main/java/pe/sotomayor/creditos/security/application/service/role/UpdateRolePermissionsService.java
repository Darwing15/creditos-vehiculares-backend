package pe.sotomayor.creditos.security.application.service.role;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRolePermissionsRequest;
import pe.sotomayor.creditos.security.application.usecase.role.UpdateRolePermissionsUseCase;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;

import pe.sotomayor.creditos.security.infrastructure.security.service.SecurityWriteGate;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.util.Set;

/**
 * Ejecuta la sustitución de permisos de un rol bajo autorización y bloqueo administrativo.
 *
 * <p>Separa la concesión de privilegios del CRUD de metadatos. Lombok genera
 * el constructor que recibe el repositorio y la barrera. La transacción
 * READ_COMMITTED permite leer concesiones vigentes después de esperar el bloqueo.</p>
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class UpdateRolePermissionsService implements UpdateRolePermissionsUseCase {
    /**
     * Recupera roles administrables y sincroniza sus concesiones con la base de datos.
     */
    private final RoleRepository roleRepository;
    /**
     * Revalida al actor y comprueba que la sustitución conserve un administrador operativo.
     */
    private final SecurityWriteGate securityWriteGate;

    /**
     * Sustituye las autoridades de un rol sin conceder capacidades superiores al actor.
     *
     * @param id      identificador del rol afectado
     * @param request conjunto completo de autoridades que debe conservar el rol
     * @return copia inmutable de los permisos resultantes
     * @throws ValidationException       si el conjunto es nulo o contiene códigos no admitidos
     * @throws ResourceNotFoundException si el rol no existe o está eliminado
     * @throws AccessDeniedException     si faltan permisos
     *                                   o el rol actual o propuesto excede los privilegios del actor
     * @throws ConflictException         si la operación
     *                                   dejaría el sistema sin administrador de seguridad
     */
    @PreAuthorize(
            "hasAuthority('security:roles:manage-permissions')")
    public Set<String> update(Long id, UpdateRolePermissionsRequest request) {
        /* actor se obtiene antes de leer el rol para evaluar la escritura con permisos actuales. */
        var actor = securityWriteGate.begin(SecurityPermission.ROLES_MANAGE_PERMISSIONS);
        if (request.permissions() == null
                || request.permissions().stream().anyMatch(p -> p == null || p.isBlank())
                || !SecurityPermission.authorities().containsAll(request.permissions())) {
            throw new ValidationException(ErrorCode.INVALID_PERMISSION,
                    "Se ha solicitado un permiso que no pertenece al catálogo.");
        }
        /* next congela la propuesta validada antes de sustituir la colección gestionada. */
        Set<String> next = Set.copyOf(request.permissions());
        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROLE_NOT_FOUND, "No existe el rol indicado."));

        /* Comprobar tanto el estado actual como el destino evita administrar o
         conceder privilegios que el actor no tiene. */
        actor.requireSubset(role.getPermissions());
        actor.requireSubset(next);
        /* Conservar la colección gestionada por JPA y reemplazar solo sus elementos. */
        role.getPermissions().clear();
        role.getPermissions().addAll(next);

        roleRepository.saveAndFlush(role);
        securityWriteGate.finish();
        return Set.copyOf(role.getPermissions());
    }
}
