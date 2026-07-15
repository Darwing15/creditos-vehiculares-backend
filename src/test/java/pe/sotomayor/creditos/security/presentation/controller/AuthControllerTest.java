package pe.sotomayor.creditos.security.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import pe.sotomayor.creditos.config.TestSecurityConfig;
import pe.sotomayor.creditos.security.application.dto.auth.request.LoginRequest;
import pe.sotomayor.creditos.security.application.dto.auth.response.LoginResponse;

import pe.sotomayor.creditos.security.application.usecase.auth.AuthenticateUserUseCase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas unitarias para {@link AuthController}.
 *
 * <p>
 * Verifica el comportamiento del endpoint encargado de autenticar
 * usuarios y emitir tokens JWT.
 * </p>
 *
 * @author DarwinG
 */
@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUserUseCase;

    @Test
    @DisplayName("Debe autenticar un usuario correctamente")
    void shouldAuthenticateUser() throws Exception {

        LoginRequest request = loginRequest();

        LoginResponse response = createResponse();

        when(authenticateUserUseCase.authenticate(request))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token")
                        .value("jwt-token"))
                .andExpect(jsonPath("$.userId")
                        .value(1))
                .andExpect(jsonPath("$.fullName")
                        .value("Raul Sotomayor"))
                .andExpect(jsonPath("$.email")
                        .value("admin@creditos.com"))
                .andExpect(jsonPath("$.roleId")
                        .value(1))
                .andExpect(jsonPath("$.roleName")
                        .value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.branchId")
                        .value(1))
                .andExpect(jsonPath("$.branchName")
                        .value("Sede Tingo"));

        verify(authenticateUserUseCase).authenticate(request);
    }

    @Test
    @DisplayName("Debe retornar 400 cuando el correo es inválido")
    void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {

        LoginRequest request = loginRequestBadEmail();

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("Debe retornar 400 cuando la contraseña está vacía")
    void shouldReturnBadRequestWhenPasswordIsBlank() throws Exception {

        LoginRequest request = loginRequestBadPassword();

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Debe retornar 400 cuando el cuerpo de la solicitud es inválido")
    void shouldReturnBadRequestWhenRequestBodyIsInvalid() throws Exception {

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{")
                )
                .andExpect(status().isBadRequest());
    }

    private LoginRequest loginRequest() {
        return new LoginRequest(
                "admin@creditos.com",
                "Admin123"
        );
    }

    private LoginRequest loginRequestBadEmail() {
        return new LoginRequest(
                "correo-invalido",
                "Admin123"
        );
    }

    private LoginRequest loginRequestBadPassword() {
        return new LoginRequest(
                "admin@creditos.com",
                ""
        );
    }

    private LoginResponse createResponse() {

        return new LoginResponse(
                "jwt-token",
                1L,
                "Raul Sotomayor",
                "admin@creditos.com",
                1L,
                "ADMINISTRADOR",
                1L,
                "Sede Tingo"
        );

    }

}
