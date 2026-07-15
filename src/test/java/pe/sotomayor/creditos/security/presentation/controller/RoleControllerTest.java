package pe.sotomayor.creditos.security.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import pe.sotomayor.creditos.security.application.dto.role.request.CreateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

import pe.sotomayor.creditos.security.application.usecase.role.CreateRoleUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.DeactivateRoleUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.ListRolesUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.UpdateRoleUseCase;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas del controlador REST de roles.
 *
 * <p>
 * Verifica el comportamiento HTTP de los endpoints expuestos
 * por {@link RoleController}.
 * </p>
 *
 * @author DarwinG
 */
@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateRoleUseCase createRoleUseCase;

    @MockitoBean
    private UpdateRoleUseCase updateRoleUseCase;

    @MockitoBean
    private ListRolesUseCase listRolesUseCase;

    @MockitoBean
    private DeactivateRoleUseCase deactivateRoleUseCase;

    @Test
    @DisplayName("Debe crear un rol correctamente")
    void shouldCreateRole() throws Exception {

        CreateRoleRequest request = createRequest();

        RoleResponse response = createResponse();

        when(createRoleUseCase.create(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/security/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Administrador"))
                .andExpect(jsonPath("$.description").
                        value("Rol con acceso completo al sistema"));

        verify(createRoleUseCase).create(any());
    }

    @Test
    @DisplayName("Debe listar roles correctamente")
    void shouldFindAllRoles() throws Exception {

        when(listRolesUseCase.findAll())
                .thenReturn(
                        List.of(createResponse())
                );

        mockMvc.perform(
                        get("/api/security/roles")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(listRolesUseCase).findAll();
    }

    @Test
    @DisplayName("Debe actualizar un rol correctamente")
    void shouldUpdateRole() throws Exception {

        UpdateRoleRequest request = updateRequest();

        RoleResponse response = updateResponse();

        when(updateRoleUseCase.update(
                any(),
                any()))
                .thenReturn(response);

        mockMvc.perform(
                        put("/api/security/roles/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Administrador"))
                .andExpect(jsonPath("$.description").
                        value("Rol con acceso completo al sistema actualizado"));

        verify(updateRoleUseCase)
                .update(any(), any());
    }

    @Test
    @DisplayName("Debe desactivar un rol correctamente")
    void shouldDeactivateRole() throws Exception {

        RoleResponse response = createResponse();

        when(deactivateRoleUseCase.deactivate(1L))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/security/roles/1/deactivate")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(1))
                .andExpect(jsonPath("$.name")
                        .value("Administrador"));

        verify(deactivateRoleUseCase)
                .deactivate(1L);

    }

    private CreateRoleRequest createRequest() {
        return new CreateRoleRequest(
                "Administrador",
                "Rol con acceso completo al sistema"
        );
    }

    private RoleResponse createResponse() {
        return new RoleResponse(
                1L,
                "Administrador",
                "Rol con acceso completo al sistema");
    }

    private UpdateRoleRequest updateRequest() {
        return new UpdateRoleRequest(
                "Administrador",
                "Rol con acceso total al sistema actualizado"
        );
    }

    private RoleResponse updateResponse() {
        return new RoleResponse(
                1L,
                "Administrador",
                "Rol con acceso completo al sistema actualizado");
    }

}