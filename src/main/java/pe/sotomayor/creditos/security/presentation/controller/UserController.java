package pe.sotomayor.creditos.security.presentation.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.request.UpdateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.response.UserFilterOptions;
import pe.sotomayor.creditos.security.application.dto.user.response.UserPageResponse;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.application.usecase.user.CreateUserUseCase;
import pe.sotomayor.creditos.security.application.usecase.user.UpdateUserUseCase;
import pe.sotomayor.creditos.security.application.usecase.user.ListUsersUseCase;
import pe.sotomayor.creditos.security.application.usecase.user.DeactivateUserUseCase;

import java.util.List;

/**
 * Controlador REST para la gestión de usuarios del sistema.
 *
 * <p>
 * Expone los endpoints necesarios para registrar, actualizar,
 * consultar y desactivar usuarios.
 * </p>
 *
 * @author DarwinG
 */
@RestController
@RequestMapping("/api/security/users")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;

    private final UpdateUserUseCase updateUserUseCase;

    private final ListUsersUseCase listUsersUseCase;

    private final DeactivateUserUseCase deactivateUserUseCase;

    /**
     * Registra un nuevo usuario.
     *
     * @param request información del usuario
     * @return usuario creado
     */
    @PostMapping
    @PreAuthorize("hasAuthority('security:users:write') " +
            "and hasAuthority('security:users:assign-role')")
    public ResponseEntity<UserResponse> create(
            @Valid
            @RequestBody
            CreateUserRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createUserUseCase.create(request));

    }

    /**
     * Actualiza un usuario existente.
     *
     * @param id      identificador del usuario
     * @param request información actualizada
     * @return usuario actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('security:users:write')")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody
            UpdateUserRequest request) {

        return ResponseEntity
                .ok(updateUserUseCase.update(id, request));

    }

    /**
     * Lista usuarios registrados.
     *
     * @return lista de usuarios
     */
    @GetMapping
    @PreAuthorize("hasAuthority('security:users:read')")
    public ResponseEntity<List<UserResponse>> findAll() {

        return ResponseEntity
                .ok(listUsersUseCase.findAll());

    }

    /**
     * Consulta una pagina global de usuarios mediante el permiso de lectura de usuarios.
     *
     * @param q        fragmento de nombre, correo o rol
     * @param roleId   identificador opcional de rol
     * @param branchId identificador opcional de sucursal
     * @param page     numero de pagina desde cero
     * @param size     tamaño de la pagina
     * @return pagina y totales de usuarios no eliminados
     */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('security:users:read')")
    public ResponseEntity<UserPageResponse> searchPage(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(listUsersUseCase
                .searchPage(q, roleId, branchId, page, size));
    }

    /**
     * Consulta las opciones de filtros asociadas a usuarios no eliminados.
     *
     * @return roles y sucursales visibles con el permiso de lectura de usuarios
     */
    @GetMapping("/filter-options")
    @PreAuthorize("hasAuthority('security:users:read')")
    public ResponseEntity<UserFilterOptions> filterOptions() {
        return ResponseEntity.ok(listUsersUseCase.filterOptions());
    }

    /**
     * Desactiva un usuario.
     *
     * @param id identificador del usuario
     * @return usuario desactivado
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('security:users:write')")
    public ResponseEntity<UserResponse> deactivate(
            @PathVariable Long id) {

        return ResponseEntity
                .ok(deactivateUserUseCase.deactivate(id));

    }

}
