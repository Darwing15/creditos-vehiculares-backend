package pe.sotomayor.creditos.customers.presentation;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import pe.sotomayor.creditos.customers.application.dto.request.CreateCustomerRequest;
import pe.sotomayor.creditos.customers.application.dto.request.UpdateCustomerRequest;

import pe.sotomayor.creditos.customers.application.dto.response.CustomerOptionResponse;
import pe.sotomayor.creditos.customers.application.dto.response.CustomerResponse;

import pe.sotomayor.creditos.customers.application.usecase.CreateCustomerUseCase;
import pe.sotomayor.creditos.customers.application.usecase.GetCustomerUseCase;
import pe.sotomayor.creditos.customers.application.usecase.ListCustomersUseCase;
import pe.sotomayor.creditos.customers.application.usecase.UpdateCustomerUseCase;

import java.util.List;
import java.util.UUID;

/**
 * API de lectura de clientes para alimentar operaciones de negocio.
 *
 * @author DarwinG
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CreateCustomerUseCase create;

    private final UpdateCustomerUseCase update;

    private final GetCustomerUseCase get;

    private final ListCustomersUseCase listCustomers;

    /**
     * GET /api/customers; devuelve clientes activos de la sucursal del usuario.
     *
     * @return lista JSON directa de opciones.
     */
    @GetMapping
    public ResponseEntity<List<CustomerOptionResponse>> listActive() {
        return ResponseEntity.ok(listCustomers.listActive());
    }

    /**
     * Consulta el detalle del registro por identidad publica.
     */
    @GetMapping("/{code}")
    public ResponseEntity<CustomerResponse> get(@PathVariable UUID code) {
        return ResponseEntity.ok(get.get(code));
    }

    /**
     * Registra en la sucursal autenticada con estado inicial del sistema.
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> create(
            @Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(201).body(create.create(request));
    }

    /**
     * Actualiza la ficha con revision; no modifica sucursal ni estado.
     */
    @PutMapping("/{code}")
    public ResponseEntity<CustomerResponse> update(
            @PathVariable UUID code,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(update.update(code, request));
    }
}
