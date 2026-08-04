package pe.sotomayor.creditos.security.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.context.annotation.Import;

import org.springframework.http.MediaType;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.test.web.servlet.MockMvc;

import pe.sotomayor.creditos.config.TestSecurityConfig;

import pe.sotomayor.creditos.security.application.dto.auth.request.ChangePasswordRequest;

import pe.sotomayor.creditos.security.application.usecase.auth.AuthenticateUserUseCase;
import pe.sotomayor.creditos.security.application.usecase.auth.ChangePasswordUseCase;
import pe.sotomayor.creditos.security.application.usecase.auth.GetAuthenticatedUserUseCase;

import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc
class ChangePasswordControllerTest {

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
    void shouldChangePasswordSuccessfully() throws Exception {

        /* Arrange */

        ChangePasswordRequest request = validRequest();

        /* Act & Assert */

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNoContent());

        verify(changePasswordUseCase).changePassword(request);
    }

    @Test
    void shouldReturnBadRequestWhenCurrentPasswordIsBlank()
            throws Exception {

        ChangePasswordRequest request =
                new ChangePasswordRequest(
                        "",
                        "NuevaPassword123"
                );

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenNewPasswordIsTooShort()
            throws Exception {

        ChangePasswordRequest request =
                new ChangePasswordRequest(
                        "Admin123",
                        "123"
                );

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenRequestBodyIsInvalid()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{")
                )
                .andExpect(status().isBadRequest());
    }

    private ChangePasswordRequest validRequest() {

        return new ChangePasswordRequest(
                "Admin123",
                "NuevaPassword123"
        );
    }
}
