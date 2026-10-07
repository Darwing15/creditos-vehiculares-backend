package pe.sotomayor.creditos.security.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import pe.sotomayor.creditos.security.application.dto.auth.request.ChangePasswordRequest;
import pe.sotomayor.creditos.security.application.dto.auth.request.LoginRequest;
import pe.sotomayor.creditos.security.application.dto.auth.response.AuthenticatedUserResponse;
import pe.sotomayor.creditos.security.application.dto.auth.response.LoginResponse;

import pe.sotomayor.creditos.security.application.usecase.auth.AuthenticateUserUseCase;
import pe.sotomayor.creditos.security.application.usecase.auth.ChangePasswordUseCase;
import pe.sotomayor.creditos.security.application.usecase.auth.GetAuthenticatedUserUseCase;
import pe.sotomayor.creditos.security.application.usecase.auth.LogoutAllUseCase;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;

import pe.sotomayor.creditos.security.infrastructure.security.config.SecurityConfig;
import pe.sotomayor.creditos.security.infrastructure.security.config.SecurityErrorHandler;
import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

import pe.sotomayor.creditos.shared.exception.GlobalExceptionHandler;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(controllers = AuthController.class, properties = {
        "app.security.allowed-origins=http://localhost:5173", "app.security.docs-enabled=false"})
@Import({SecurityConfig.class, SecurityErrorHandler.class, GlobalExceptionHandler.class})
class AuthControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @MockitoBean
    private JwtService jwt;
    @MockitoBean
    private CustomUserDetailsService users;
    @MockitoBean
    private AuthenticateUserUseCase login;
    @MockitoBean
    private GetAuthenticatedUserUseCase me;
    @MockitoBean
    private ChangePasswordUseCase change;
    @MockitoBean
    private LogoutAllUseCase logout;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnTokenContractWhenLoginSucceeds() throws Exception {
        /* Arrange */
        LoginRequest request = createLoginRequest();
        LoginResponse expected = createLoginResponse();
        when(login.authenticate(request)).thenReturn(expected);

        /* Act */
        MvcResult result = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readValue(result.getResponse().getContentAsString(), LoginResponse.class)).isEqualTo(expected);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.has("password")).isFalse();
        assertThat(result.getRequest().getSession(false)).isNull();
        verify(login).authenticate(request);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        /* Arrange */
        LoginRequest valid = createLoginRequest();
        LoginRequest request = new LoginRequest("invalid", valid.password());

        /* Act */
        MvcResult result = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(login, me, change, logout, jwt, users);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldReturnBadRequestWhenPasswordIsBlank() throws Exception {
        /* Arrange */
        LoginRequest valid = createLoginRequest();
        LoginRequest request = new LoginRequest(valid.email(), " ");

        /* Act */
        MvcResult result = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(login, me, change, logout, jwt, users);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldReturnBadRequestWhenPasswordIsTooLong() throws Exception {
        /* Arrange */
        LoginRequest valid = createLoginRequest();
        LoginRequest request = new LoginRequest(valid.email(), "a".repeat(257));

        /* Act */
        MvcResult result = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(login, me, change, logout, jwt, users);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldReturnControlledErrorWhenCredentialsAreInvalid() throws Exception {
        /* Arrange */
        LoginRequest request = createLoginRequest();
        when(login.authenticate(request)).thenThrow(new BadCredentialsException("private-detail"));

        /* Act */
        MvcResult result = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(body.path("message").asText()).doesNotContain("private-detail");
        verify(login).authenticate(request);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldReturnControlledErrorWhenAccountIsDisabled() throws Exception {
        /* Arrange */
        LoginRequest request = createLoginRequest();
        when(login.authenticate(request)).thenThrow(new DisabledException("private-detail"));

        /* Act */
        MvcResult result = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(body.path("message").asText()).doesNotContain("private-detail");
        verify(login).authenticate(request);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldReturnControlledErrorWhenProviderIsUnavailable() throws Exception {
        /* Arrange */
        LoginRequest request = createLoginRequest();
        when(login.authenticate(request)).thenThrow(new AuthenticationServiceException("private-detail"));

        /* Act */
        MvcResult result = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(503);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.path("message").asText()).doesNotContain("private-detail");
        verify(login).authenticate(request);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldReturnBadRequestWhenJsonIsMalformed() throws Exception {
        /* Arrange */
        String payload = "{";

        /* Act */
        MvcResult result = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        verifyNoInteractions(login, me, change, logout, jwt, users);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldReturnIdentityAndPermissionsWhenMeIsAuthenticated() throws Exception {
        /* Arrange */
        AuthenticatedUserResponse expected = createMeResponse();
        when(me.getCurrentUser()).thenReturn(expected);

        /* Act */
        MvcResult result = mvc.perform(get("/api/auth/me").with(user(createPrincipal()))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readValue(result.getResponse().getContentAsString(),
                AuthenticatedUserResponse.class)).isEqualTo(expected);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.has("password")).isFalse();
        verify(me).getCurrentUser();
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldReturnUnauthorizedWhenMeHasNoAuthentication() throws Exception {
        /* Arrange */
        String endpoint = "/api/auth/me";

        /* Act */
        MvcResult result = mvc.perform(get(endpoint)).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(result.getResponse().getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        verifyNoInteractions(login, me, change, logout, jwt, users);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldReturnUnauthorizedWhenChangePasswordHasNoAuthentication() throws Exception {
        /* Arrange */
        String endpoint = "/api/auth/change-password";

        /* Act */
        MvcResult result = mvc.perform(post(endpoint)).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(result.getResponse().getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        verifyNoInteractions(login, me, change, logout, jwt, users);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldReturnUnauthorizedWhenLogoutAllHasNoAuthentication() throws Exception {
        /* Arrange */
        String endpoint = "/api/auth/logout-all";

        /* Act */
        MvcResult result = mvc.perform(post(endpoint)).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(result.getResponse().getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        verifyNoInteractions(login, me, change, logout, jwt, users);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldReturnNoContentWhenPasswordChangeSucceeds() throws Exception {
        /* Arrange */
        ChangePasswordRequest request = createChangeRequest();

        /* Act */
        MvcResult result = mvc.perform(post("/api/auth/change-password")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(result.getResponse().getContentAsString()).isEmpty();
        verify(change).changePassword(request);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldReturnNoContentWhenLogoutAllSucceeds() throws Exception {
        /* Arrange */
        SecurityPrincipal principal = createPrincipal();

        /* Act */
        MvcResult result = mvc.perform(post("/api/auth/logout-all").with(user(principal))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(result.getResponse().getContentAsString()).isEmpty();
        verify(logout).logoutAll();
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "ááááááááááááááááááááááááááááááááááááá"})
    void shouldRejectPasswordWhenByteOrCharacterLimitIsViolated(String password) throws Exception {
        /* Arrange */
        ChangePasswordRequest valid = createChangeRequest();
        ChangePasswordRequest request = new ChangePasswordRequest(valid.currentPassword(), password);

        /* Act */
        MvcResult result = mvc.perform(post("/api/auth/change-password")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        verifyNoInteractions(login, me, change, logout, jwt, users);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldAcceptPasswordWhenUtf8LengthIsExactly72Bytes() throws Exception {
        /* Arrange */
        ChangePasswordRequest valid = createChangeRequest();
        ChangePasswordRequest request = new ChangePasswordRequest(valid.currentPassword(), "á".repeat(36));

        /* Act */
        MvcResult result = mvc.perform(post("/api/auth/change-password")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        verify(change).changePassword(request);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldAllowPreflightWhenOriginIsConfigured() throws Exception {
        /* Arrange */
        String origin = "http://localhost:5173";

        /* Act */
        MvcResult result = mvc.perform(options("/api/auth/login")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin")).isEqualTo(origin);
        assertThat(result.getResponse().getHeader("Access-Control-Allow-Credentials")).isNull();
        verifyNoInteractions(login, me, change, logout, jwt, users);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @Test
    void shouldRejectPreflightWhenOriginIsUnknown() throws Exception {
        /* Arrange */
        String origin = "https://unknown.test";

        /* Act */
        MvcResult result = mvc.perform(options("/api/auth/login")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "POST"))
                .andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();
        verifyNoInteractions(login, me, change, logout, jwt, users);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/swagger-ui/index.html", "/api/unknown"})
    void shouldDenyRouteWhenNotExplicitlyEnabled(String endpoint) throws Exception {
        /* Arrange */
        SecurityPrincipal principal = createPrincipal();

        /* Act */
        MvcResult result = mvc.perform(get(endpoint).with(user(principal))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        verifyNoInteractions(login, me, change, logout, jwt, users);
        verifyNoMoreInteractions(login, me, change, logout, jwt, users);
    }

    private SecurityPrincipal createPrincipal() {
        return new SecurityPrincipal(1L, 1L, "Ana Perez", "ana@test.com", "encoded-password", 2L,
                "Operador", 3L, "Lima", true, SecurityPermission.authorities().stream()
                .map(SimpleGrantedAuthority::new).toList());
    }

    private LoginRequest createLoginRequest() {
        return new LoginRequest("ana@test.com", "a valid password");
    }

    private ChangePasswordRequest createChangeRequest() {
        return new ChangePasswordRequest("current password", "a new strong password");
    }

    private LoginResponse createLoginResponse() {
        return new LoginResponse(
                "signed-token",
                1L,
                "Ana Perez",
                "ana@test.com",
                2L,
                "Operador",
                3L,
                "Lima",
                "Bearer",
                600L
        );
    }

    private AuthenticatedUserResponse createMeResponse() {
        return new AuthenticatedUserResponse(
                1L,
                "Ana Perez",
                "ana@test.com",
                2L,
                "Operador",
                3L,
                "Lima",
                List.of("security:users:read")
        );
    }

}

