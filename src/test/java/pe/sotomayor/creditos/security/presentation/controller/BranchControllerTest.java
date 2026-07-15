package pe.sotomayor.creditos.security.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.test.web.servlet.MockMvc;

import pe.sotomayor.creditos.security.application.dto.branch.request.CreateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.request.UpdateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

import pe.sotomayor.creditos.security.application.usecase.branch.CreateBranchUseCase;
import pe.sotomayor.creditos.security.application.usecase.branch.UpdateBranchUseCase;
import pe.sotomayor.creditos.security.application.usecase.branch.ListBranchesUseCase;
import pe.sotomayor.creditos.security.application.usecase.branch.DeactivateBranchUseCase;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias para {@link BranchController}.
 *
 * <p>
 * Verifica el comportamiento HTTP del controlador encargado
 * de gestionar sucursales.
 * </p>
 *
 * @author DarwinG
 */
@WebMvcTest(BranchController.class)
@AutoConfigureMockMvc(addFilters = false)
class BranchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateBranchUseCase createBranchUseCase;

    @MockitoBean
    private UpdateBranchUseCase updateBranchUseCase;

    @MockitoBean
    private ListBranchesUseCase listBranchesUseCase;

    @MockitoBean
    private DeactivateBranchUseCase deactivateBranchUseCase;

    @Test
    @DisplayName("Debe crear una sucursal correctamente")
    void shouldCreateBranch() throws Exception {

        CreateBranchRequest request = createRequest();

        BranchResponse response = createResponse();

        when(createBranchUseCase.create(request))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/security/branches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code")
                        .value("TOC01"))
                .andExpect(jsonPath("$.name")
                        .value("Sucursal Tocache"));

        verify(createBranchUseCase).create(request);

    }

    @Test
    @DisplayName("Debe listar sucursales correctamente")
    void shouldFindAllBranches() throws Exception {

        when(listBranchesUseCase.findAll())
                .thenReturn(
                        List.of(createResponse())
                );

        mockMvc.perform(
                        get("/api/security/branches")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code")
                        .value("TOC01"));

        verify(listBranchesUseCase).findAll();
    }

    @Test
    @DisplayName("Debe actualizar una sucursal correctamente")
    void shouldUpdateBranch() throws Exception {

        UpdateBranchRequest request = updateRequest();

        BranchResponse response = createResponse();

        when(updateBranchUseCase.update(
                1L,
                request
        ))
                .thenReturn(response);

        mockMvc.perform(
                        put("/api/security/branches/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(1));

        verify(updateBranchUseCase).update(1L, request);
    }

    @Test
    @DisplayName("Debe desactivar una sucursal correctamente")
    void shouldDeactivateBranch() throws Exception {

        BranchResponse response = deactivateResponse();

        when(deactivateBranchUseCase.deactivate(1L))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/security/branches/1/deactivate")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active")
                        .value(false));

        verify(deactivateBranchUseCase).deactivate(1L);
    }

    private CreateBranchRequest createRequest() {
        return new CreateBranchRequest(
                "TOC01",
                "Sucursal Tocache",
                "Av. Principal 123",
                "987654321",
                "tocache@creditos.com",
                null
        );
    }

    private BranchResponse createResponse() {

        return new BranchResponse(
                1L,
                "TOC01",
                "Sucursal Tocache",
                "Av. Principal 123",
                "987654321",
                "tocache@creditos.com",
                null,
                true
        );
    }

    private UpdateBranchRequest updateRequest() {
        return new UpdateBranchRequest(
                "Sucursal Tocache Actualizada",
                "Av. Principal 456",
                "999999999",
                "tocache@creditos.com",
                null
        );
    }

    private BranchResponse deactivateResponse() {
        return new BranchResponse(
                1L,
                "TOC01",
                "Sucursal Tocache",
                "Av. Principal 123",
                "987654321",
                "tocache@creditos.com",
                null,
                false
        );
    }

}
