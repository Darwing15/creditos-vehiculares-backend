package pe.sotomayor.creditos.security.application.usecase.role;

import org.springframework.security.access.AccessDeniedException;

import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import java.util.Set;

/**
 * Expone la consulta de permisos de un rol sin ampliar su DTO de metadatos.
 *
 * @author DarwinG
 */
public interface GetRolePermissionsUseCase {
    /**
     * Consulta las concesiones del rol solicitado bajo autorización de lectura.
     *
     * @param id identificador del rol
     * @return permisos asignados, o un conjunto vacío si no posee concesiones
     * @throws ResourceNotFoundException si el rol no está disponible
     * @throws AccessDeniedException     si el actor carece de permiso de lectura
     */
    Set<String> getById(Long id);
}
