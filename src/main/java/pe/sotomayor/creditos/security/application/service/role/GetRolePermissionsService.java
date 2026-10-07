package pe.sotomayor.creditos.security.application.service.role;

import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.prepost.PreAuthorize;

import pe.sotomayor.creditos.security.application.usecase.role.GetRolePermissionsUseCase;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

/**
 * Consulta las autoridades de un rol dentro de una transacción de solo lectura.
 *
 * <p>La transacción permite resolver la colección diferida antes de devolver
 * una copia independiente de JPA. Lombok genera el constructor que recibe roles.</p>
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRolePermissionsService implements GetRolePermissionsUseCase {
    /**
     * Busca el rol aplicando explícitamente la exclusión del borrado lógico.
     */
    private final RoleRepository roles;

    /**
     * Obtiene los permisos de un rol administrable sin exponer la colección de la entidad.
     *
     * @param id identificador del rol
     * @return copia inmutable de sus autoridades, que puede estar vacía
     * @throws ResourceNotFoundException si el rol no existe o está eliminado
     * @throws AccessDeniedException     si falta autorización de lectura
     */
    @Override
    @PreAuthorize("hasAuthority('security:roles:read')")
    public Set<String> getById(Long id) {
        /* La entidad solo se utiliza dentro de esta transacción de lectura. */
        var role = roles.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROLE_NOT_FOUND, "No existe el rol indicado."));
        return Set.copyOf(role.getPermissions());
    }
}
