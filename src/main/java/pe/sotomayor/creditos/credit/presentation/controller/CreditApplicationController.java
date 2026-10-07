package pe.sotomayor.creditos.credit.presentation.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.request.CreateCreditApplicationRequest;
import pe.sotomayor.creditos.credit.application.dto.creditapplication.request.RejectCreditApplicationRequest;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.application.dto.creditsimulation.request.CreditSimulationRequest;
import pe.sotomayor.creditos.credit.application.dto.creditsimulation.response.CreditSimulationResponse;

import pe.sotomayor.creditos.credit.application.usecase.creditapplication.ApproveCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.CancelCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.CreateCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.GetCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.ListCreditApplicationsUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.RejectCreditApplicationUseCase;

import pe.sotomayor.creditos.credit.application.usecase.creditsimulation.SimulateCreditApplicationUseCase;

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de solicitudes de crédito.
 *
 * <p>
 * Expone los endpoints HTTP asociados al agregado
 * CreditApplication y delega la ejecución de los casos de uso
 * a la capa Application.
 * </p>
 *
 * @author DarwinG
 */
@RestController
@RequestMapping("/api/credit-applications")
@RequiredArgsConstructor
public class CreditApplicationController {

    private final CreateCreditApplicationUseCase
            createCreditApplicationUseCase;

    private final ApproveCreditApplicationUseCase
            approveCreditApplicationUseCase;

    private final RejectCreditApplicationUseCase
            rejectCreditApplicationUseCase;

    private final CancelCreditApplicationUseCase
            cancelCreditApplicationUseCase;

    private final GetCreditApplicationUseCase
            getCreditApplicationUseCase;

    private final ListCreditApplicationsUseCase
            listCreditApplicationsUseCase;

    private final SimulateCreditApplicationUseCase
            simulateCreditApplicationUseCase;

    /**
     * Registra una nueva solicitud de crédito.
     *
     * @param request información requerida para registrar
     *                la solicitud.
     * @return solicitud de crédito creada.
     */
    @PostMapping
    public ResponseEntity<CreditApplicationResponse> create(
            @Valid
            @RequestBody
            CreateCreditApplicationRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        createCreditApplicationUseCase.create(
                                request
                        )
                );
    }

    /**
     * Aprueba una solicitud de crédito pendiente.
     *
     * @param applicationCode código público de la solicitud.
     * @return solicitud de crédito aprobada.
     */
    @PatchMapping("/{applicationCode}/approve")
    public ResponseEntity<CreditApplicationResponse> approve(
            @PathVariable UUID applicationCode) {

        return ResponseEntity.ok(
                approveCreditApplicationUseCase.approve(
                        applicationCode
                )
        );
    }

    /**
     * Rechaza una solicitud de crédito pendiente.
     *
     * @param applicationCode código público de la solicitud.
     * @param request         información requerida para efectuar el rechazo.
     * @return solicitud de crédito rechazada.
     */
    @PatchMapping("/{applicationCode}/reject")
    public ResponseEntity<CreditApplicationResponse> reject(
            @PathVariable UUID applicationCode,
            @Valid
            @RequestBody
            RejectCreditApplicationRequest request) {

        return ResponseEntity.ok(
                rejectCreditApplicationUseCase.reject(
                        applicationCode,
                        request
                )
        );
    }

    /**
     * Cancela una solicitud de crédito pendiente.
     *
     * @param applicationCode código público de la solicitud.
     * @return solicitud de crédito cancelada.
     */
    @PatchMapping("/{applicationCode}/cancel")
    public ResponseEntity<CreditApplicationResponse> cancel(
            @PathVariable UUID applicationCode) {

        return ResponseEntity.ok(
                cancelCreditApplicationUseCase.cancel(
                        applicationCode
                )
        );
    }

    /**
     * Obtiene una solicitud de crédito mediante su código público.
     *
     * @param applicationCode código público de la solicitud.
     * @return solicitud de crédito encontrada.
     */
    @GetMapping("/{applicationCode}")
    public ResponseEntity<CreditApplicationResponse>
    getByApplicationCode(
            @PathVariable UUID applicationCode) {

        return ResponseEntity.ok(
                getCreditApplicationUseCase
                        .getByApplicationCode(applicationCode)
        );
    }

    /**
     * Obtiene las solicitudes de crédito que se encuentran
     * en el estado indicado.
     *
     * @param status estado de las solicitudes a consultar.
     * @return solicitudes de crédito encontradas.
     */
    @GetMapping
    public ResponseEntity<List<CreditApplicationResponse>>
    listByStatus(
            @RequestParam CreditApplicationStatus status) {

        return ResponseEntity.ok(
                listCreditApplicationsUseCase
                        .listByStatus(status)
        );
    }

    /**
     * Calcula una cuota estimada sin guardar solicitud ni crédito.
     *
     * @param request condiciones financieras tentativas
     * @return resumen calculado con los servicios de dominio
     */
    @PostMapping("/simulations")
    public ResponseEntity<CreditSimulationResponse> simulate(
            @Valid @RequestBody CreditSimulationRequest request) {
        return ResponseEntity.ok(
                simulateCreditApplicationUseCase.simulate(request)
        );
    }

}
