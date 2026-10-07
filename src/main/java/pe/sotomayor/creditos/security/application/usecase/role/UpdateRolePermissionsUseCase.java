package pe.sotomayor.creditos.security.application.usecase.role;

import org.springframework.security.access.AccessDeniedException;

import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRolePermissionsRequest;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.util.Set;

/**
 * Define la operación de asignación de permisos que consumen los adaptadores de entrada.
 *
 * <p>El controlador depende de este contrato y no de la clase de servicio concreta.</p>
 *
 * @author DarwinG
 */
public interface UpdateRolePermissionsUseCase {
    /**
     * Reemplaza las concesiones del rol, aplicando autorización e invariantes administrativas.
     *
     * @param id      identificador del rol que se modifica
     * @param request conjunto completo de permisos destino
     * @return permisos efectivos después de persistir la modificación
     * @throws ValidationException       si hay permisos inválidos
     * @throws ResourceNotFoundException si el rol no está disponible
     * @throws AccessDeniedException     si la concesión no está autorizada
     * @throws ConflictException         si se pierde el último administrador
     */
    Set<String> update(Long id, UpdateRolePermissionsRequest request);
}
