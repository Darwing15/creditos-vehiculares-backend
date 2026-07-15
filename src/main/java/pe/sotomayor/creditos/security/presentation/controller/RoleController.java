package pe.sotomayor.creditos.security.presentation.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import pe.sotomayor.creditos.security.application.dto.role.request.CreateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

import pe.sotomayor.creditos.security.application.usecase.role.CreateRoleUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.UpdateRoleUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.ListRolesUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.DeactivateRoleUseCase;

import java.util.List;

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
     * Registra un nuevo rol.
     *
     * @param request información del rol
     * @return rol creado
     */
    @PostMapping
    public ResponseEntity<RoleResponse> create(
            @Valid @RequestBody CreateRoleRequest request
    ) {
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
    public ResponseEntity<RoleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
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
    public ResponseEntity<RoleResponse> deactivate(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                deactivateRoleUseCase.deactivate(id)
        );

    }
}
