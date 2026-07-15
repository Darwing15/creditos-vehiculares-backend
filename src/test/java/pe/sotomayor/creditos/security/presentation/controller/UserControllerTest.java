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

import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.request.UpdateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.application.usecase.user.CreateUserUseCase;
import pe.sotomayor.creditos.security.application.usecase.user.UpdateUserUseCase;
import pe.sotomayor.creditos.security.application.usecase.user.ListUsersUseCase;
import pe.sotomayor.creditos.security.application.usecase.user.DeactivateUserUseCase;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias para {@link UserController}.
 *
 * <p>
 * Verifica el comportamiento de los endpoints REST encargados
 * de la gestión de usuarios.
 * </p>
 *
 * @author DarwinG
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateUserUseCase createUserUseCase;

    @MockitoBean
    private UpdateUserUseCase updateUserUseCase;

    @MockitoBean
    private ListUsersUseCase listUsersUseCase;

    @MockitoBean
    private DeactivateUserUseCase deactivateUserUseCase;

    @Test
    @DisplayName("Debe crear un usuario correctamente")
    void shouldCreateUser() throws Exception {

        CreateUserRequest request = createRequest();

        UserResponse response = createResponse();

        when(createUserUseCase.create(request))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/security/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(1))
                .andExpect(jsonPath("$.email")
                        .value("darwin@creditos.com"));

        verify(createUserUseCase)
                .create(request);

    }

    @Test
    @DisplayName("Debe listar usuarios correctamente")
    void shouldFindAllUsers() throws Exception {

        when(listUsersUseCase.findAll())
                .thenReturn(
                        List.of(createResponse())
                );

        mockMvc.perform(
                        get("/api/security/users")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName")
                        .value("Raul Sotomayor"));

        verify(listUsersUseCase)
                .findAll();

    }

    @Test
    @DisplayName("Debe actualizar un usuario correctamente")
    void shouldUpdateUser() throws Exception {

        UpdateUserRequest request = updateRequest();

        UserResponse response = createResponse();

        when(updateUserUseCase.update(
                1L,
                request
        ))
                .thenReturn(response);

        mockMvc.perform(
                        put("/api/security/users/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active")
                        .value(true));

        verify(updateUserUseCase)
                .update(1L, request);

    }

    @Test
    @DisplayName("Debe desactivar un usuario correctamente")
    void shouldDeactivateUser() throws Exception {

        UserResponse response = deactivateResponse();

        when(deactivateUserUseCase.deactivate(1L))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/security/users/1/deactivate")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active")
                        .value(false));

        verify(deactivateUserUseCase)
                .deactivate(1L);

    }

    private CreateUserRequest createRequest() {
        return new CreateUserRequest(
                "Raul Sotomayor",
                "darwin@creditos.com",
                "Password123",
                1L,
                1L
        );
    }

    private UserResponse createResponse() {

        return new UserResponse(
                1L,
                "Raul Sotomayor",
                "darwin@creditos.com",
                1L,
                "Administrador",
                1L,
                "Sucursal Tocache",
                true
        );
    }

    private UpdateUserRequest updateRequest() {
        return new UpdateUserRequest(
                "Raul Sotomayor",
                "darwin@creditos.com",
                1L,
                1L
        );
    }

    private UserResponse deactivateResponse() {

        return new UserResponse(
                1L,
                "Raul Sotomayor",
                "darwin@creditos.com",
                1L,
                "Administrador",
                1L,
                "Sucursal Tocache",
                false
        );
    }

}
