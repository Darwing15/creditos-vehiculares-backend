package pe.sotomayor.creditos.security.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import pe.sotomayor.creditos.security.application.usecase.auth.ChangePasswordUseCase;
import pe.sotomayor.creditos.security.application.usecase.auth.GetAuthenticatedUserUseCase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc
class AuthLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUserUseCase;

    @MockitoBean
    private GetAuthenticatedUserUseCase getAuthenticatedUserUseCase;

    @MockitoBean
    private ChangePasswordUseCase changePasswordUseCase;

    @Test
    void shouldAuthenticateUser() throws Exception {

        /* Arrange */

        LoginRequest request = loginRequest();

        LoginResponse response = createResponse();

        when(authenticateUserUseCase.authenticate(request))
                .thenReturn(response);

        /* Act & Assert */

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
