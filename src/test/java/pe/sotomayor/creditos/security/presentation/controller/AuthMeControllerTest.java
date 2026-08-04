package pe.sotomayor.creditos.security.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.context.annotation.Import;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.test.web.servlet.MockMvc;

import pe.sotomayor.creditos.config.TestSecurityConfig;

import pe.sotomayor.creditos.security.application.dto.auth.response.AuthenticatedUserResponse;

import pe.sotomayor.creditos.security.application.usecase.auth.AuthenticateUserUseCase;
import pe.sotomayor.creditos.security.application.usecase.auth.ChangePasswordUseCase;
import pe.sotomayor.creditos.security.application.usecase.auth.GetAuthenticatedUserUseCase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc
class AuthMeControllerTest {

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
    void shouldReturnAuthenticatedUser() throws Exception {

        /* Arrange */

        AuthenticatedUserResponse response =
                createResponse();

        when(getAuthenticatedUserUseCase.getCurrentUser())
                .thenReturn(response);

        /* Act & Assert */

        mockMvc.perform(
                        get("/api/auth/me")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
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

        verify(getAuthenticatedUserUseCase).getCurrentUser();
    }

    private AuthenticatedUserResponse createResponse() {

        return new AuthenticatedUserResponse(
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