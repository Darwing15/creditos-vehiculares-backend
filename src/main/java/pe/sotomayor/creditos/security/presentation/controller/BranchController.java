package pe.sotomayor.creditos.security.presentation.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import pe.sotomayor.creditos.security.application.dto.branch.request.CreateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.request.UpdateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

import pe.sotomayor.creditos.security.application.usecase.branch.CreateBranchUseCase;
import pe.sotomayor.creditos.security.application.usecase.branch.UpdateBranchUseCase;
import pe.sotomayor.creditos.security.application.usecase.branch.ListBranchesUseCase;
import pe.sotomayor.creditos.security.application.usecase.branch.DeactivateBranchUseCase;

import java.util.List;

/**
 * Controlador REST para la gestión de sucursales.
 *
 * <p>
 * Expone los endpoints HTTP asociados al agregado Branch,
 * delegando la lógica de negocio a la capa Application.
 * </p>
 *
 * @author DarwinG
 */
@RestController
@RequestMapping("/api/security/branches")
@RequiredArgsConstructor
public class BranchController {

    private final CreateBranchUseCase createBranchUseCase;

    private final UpdateBranchUseCase updateBranchUseCase;

    private final ListBranchesUseCase listBranchesUseCase;

    private final DeactivateBranchUseCase deactivateBranchUseCase;

    /**
     * Registra una nueva sucursal.
     *
     * @param request información de la sucursal
     * @return sucursal creada
     */
    @PostMapping
    public ResponseEntity<BranchResponse> create(
            @Valid
            @RequestBody
            CreateBranchRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createBranchUseCase.create(request));
    }

    /**
     * Obtiene las sucursales registradas.
     *
     * @return lista de sucursales
     */
    @GetMapping
    public ResponseEntity<List<BranchResponse>> findAll() {

        return ResponseEntity.ok(
                listBranchesUseCase.findAll()
        );
    }

    /**
     * Actualiza una sucursal existente.
     *
     * @param id      identificador de sucursal
     * @param request información actualizada
     * @return sucursal actualizada
     */
    @PutMapping("/{id}")
    public ResponseEntity<BranchResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody
            UpdateBranchRequest request
    ) {
        return ResponseEntity.ok(
                updateBranchUseCase.update(id, request)
        );
    }

    /**
     * Desactiva una sucursal.
     *
     * @param id identificador de sucursal
     * @return sucursal desactivada
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<BranchResponse> deactivate(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                deactivateBranchUseCase.deactivate(id)
        );
    }

}
