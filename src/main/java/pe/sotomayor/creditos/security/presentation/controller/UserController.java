package pe.sotomayor.creditos.security.presentation.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.request.UpdateUserRequest;
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
    public ResponseEntity<UserResponse> create(
            @Valid
            @RequestBody
            CreateUserRequest request
    ) {
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
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody
            UpdateUserRequest request
    ) {
        return ResponseEntity
                .ok(updateUserUseCase.update(id, request));

    }

    /**
     * Lista usuarios registrados.
     *
     * @return lista de usuarios
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> findAll() {

        return ResponseEntity
                .ok(listUsersUseCase.findAll());

    }

    /**
     * Desactiva un usuario.
     *
     * @param id identificador del usuario
     * @return usuario desactivado
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivate(
            @PathVariable Long id
    ) {
        return ResponseEntity
                .ok(deactivateUserUseCase.deactivate(id));

    }

}
