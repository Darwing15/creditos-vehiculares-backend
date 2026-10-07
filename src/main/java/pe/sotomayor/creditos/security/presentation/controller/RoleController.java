package pe.sotomayor.creditos.security.presentation.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.validation.Valid;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.sotomayor.creditos.security.application.dto.role.request.CreateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRolePermissionsRequest;
import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRoleRequest;

import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

import pe.sotomayor.creditos.security.application.usecase.role.CreateRoleUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.DeactivateRoleUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.GetRolePermissionsUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.ListRolesUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.UpdateRoleUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.UpdateRolePermissionsUseCase;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.util.List;
import java.util.Set;

/**
 * Controlador REST para la gestión de roles del sistema.
 *
 * <p>
 * Expone los endpoints HTTP asociados al agregado Role.
 * La lógica de negocio permanece delegada en los casos de uso
 * de la capa Application.
 * </p>
 *
 * @author DarwinG
 */
@RestController
@RequestMapping("/api/security/roles")
@RequiredArgsConstructor
public class RoleController {

    private final CreateRoleUseCase createRoleUseCase;

    private final UpdateRoleUseCase updateRoleUseCase;

    private final ListRolesUseCase listRolesUseCase;

    private final DeactivateRoleUseCase deactivateRoleUseCase;

    /**
     * Caso de uso que aplica la sustitución autorizada y transaccional de permisos.
     */
    private final UpdateRolePermissionsUseCase updateRolePermissionsUseCase;

    /**
     * Caso de uso que obtiene las concesiones de un rol sin modificar sus metadatos.
     */
    private final GetRolePermissionsUseCase getRolePermissionsUseCase;

    /**
     * Registra un nuevo rol.
     *
     * @param request información del rol
     * @return rol creado
     */
    @PostMapping
    @PreAuthorize("hasAuthority('security:roles:write')")
    public ResponseEntity<RoleResponse> create(
            @Valid @RequestBody CreateRoleRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createRoleUseCase.create(request));

    }

    /**
     * Obtiene todos los roles registrados.
     *
     * @return lista de roles
     */
    @GetMapping
    @PreAuthorize("hasAuthority('security:roles:read')")
    public ResponseEntity<List<RoleResponse>> findAll() {

        return ResponseEntity.ok(
                listRolesUseCase.findAll()
        );
    }

    /**
     * Actualiza un rol existente.
     *
     * @param id      identificador del rol
     * @param request información actualizada
     * @return rol actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('security:roles:write')")
    public ResponseEntity<RoleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {

        return ResponseEntity.ok(
                updateRoleUseCase.update(id, request)
        );
    }

    /**
     * Desactiva un rol existente.
     *
     * @param id identificador del rol
     * @return rol desactivado
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('security:roles:write')")
    public ResponseEntity<RoleResponse> deactivate(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                deactivateRoleUseCase.deactivate(id)
        );

    }

    /**
     * Reemplaza los permisos del rol identificado por la ruta.
     *
     * @param id      identificador del rol afectado
     * @param request conjunto completo de permisos destino, validado desde el cuerpo HTTP
     * @return respuesta 200 con las autoridades que conserva el rol
     * @throws AccessDeniedException     si falta autorización
     * @throws ValidationException       si hay códigos inválidos
     * @throws ResourceNotFoundException si el rol no está disponible
     * @throws ConflictException         si se pierde el último administrador
     */
    @PutMapping("/{id}/permissions")
    @PreAuthorize(
            "hasAuthority('security:roles:manage-permissions')")
    public ResponseEntity<Set<String>> updatePermissions(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return ResponseEntity.ok(updateRolePermissionsUseCase.update(id, request));
    }

    /**
     * Consulta las autoridades que están asignadas a un rol específico.
     *
     * @param id identificador del rol consultado
     * @return respuesta 200 con las concesiones actuales, que pueden estar vacías
     * @throws ResourceNotFoundException si el rol no está disponible
     * @throws AccessDeniedException     si falta permiso de lectura
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize(
            "hasAuthority('security:roles:read')")
    public ResponseEntity<Set<String>> getPermissions(@PathVariable Long id) {
        return ResponseEntity.ok(getRolePermissionsUseCase.getById(id));
    }

    /**
     * Expone los códigos disponibles para construir el selector de permisos del frontend.
     *
     * @return respuesta 200 con el catálogo completo ordenado alfabéticamente
     * @throws AccessDeniedException si falta permiso de lectura
     */
    @GetMapping("/permissions")
    @PreAuthorize(
            "hasAuthority('security:roles:read')")
    public ResponseEntity<List<String>> permissionCatalog() {
        return ResponseEntity.ok(SecurityPermission.authorities().stream().sorted().toList());
    }
}
