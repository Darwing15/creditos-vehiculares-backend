package pe.sotomayor.creditos.vehicles.presentation;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.sotomayor.creditos.vehicles.application.dto.request.CreateVehicleRequest;
import pe.sotomayor.creditos.vehicles.application.dto.request.UpdateVehicleRequest;

import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleOptionResponse;
import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleOverviewResponse;
import pe.sotomayor.creditos.vehicles.application.dto.response.VehiclePageResponse;
import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleResponse;

import pe.sotomayor.creditos.vehicles.application.usecase.CreateVehicleUseCase;
import pe.sotomayor.creditos.vehicles.application.usecase.GetVehicleUseCase;
import pe.sotomayor.creditos.vehicles.application.usecase.ListVehiclesUseCase;
import pe.sotomayor.creditos.vehicles.application.usecase.UpdateVehicleUseCase;

import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;

import java.util.List;
import java.util.UUID;

/**
 * API de lectura de inventario por sucursal.
 *
 * @author DarwinG
 */
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final CreateVehicleUseCase create;

    private final GetVehicleUseCase get;

    private final ListVehiclesUseCase listVehicles;

    private final UpdateVehicleUseCase update;

    /**
     * GET /api/vehicles?status=AVAILABLE; status omitido lista todos.
     *
     * @param status filtro validado por Spring.
     * @return lista JSON directa.
     */
    @GetMapping
    public ResponseEntity<List<VehicleOptionResponse>> list(
            @RequestParam(required = false) VehicleStatus status) {
        return ResponseEntity.ok(listVehicles.list(status));
    }

    /**
     * Consulta inventario paginado de la sucursal autenticada.
     *
     * @param q      fragmento literal de busqueda
     * @param status estado opcional
     * @param brand  marca opcional
     * @param page   numero de pagina desde cero
     * @param size   tamaño de la pagina
     * @return pagina y totales del filtro
     */
    @GetMapping("/page")
    public ResponseEntity<VehiclePageResponse> listPage(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(defaultValue = "") String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(listVehicles.searchPage(q, status, brand, page, size));
    }

    /**
     * Consulta indicadores y marcas de toda la sucursal autenticada.
     *
     * @return resumen de inventario sin aplicar filtros del listado
     */
    @GetMapping("/overview")
    public ResponseEntity<VehicleOverviewResponse> overview() {
        return ResponseEntity.ok(listVehicles.overview());
    }

    /**
     * Consulta el detalle del registro por identidad publica.
     */
    @GetMapping("/{code}")
    public ResponseEntity<VehicleResponse> get(@PathVariable UUID code) {
        return ResponseEntity.ok(get.get(code));
    }

    /**
     * Registra en la sucursal autenticada con estado inicial del sistema.
     */
    @PostMapping
    public ResponseEntity<VehicleResponse> create(
            @Valid @RequestBody CreateVehicleRequest request) {
        return ResponseEntity.status(201).body(create.create(request));
    }

    /**
     * Actualiza la ficha con revision; no modifica sucursal ni estado.
     */
    @PutMapping("/{code}")
    public ResponseEntity<VehicleResponse> update(
            @PathVariable UUID code,
            @Valid @RequestBody UpdateVehicleRequest request) {
        return ResponseEntity.ok(update.update(code, request));
    }
}
