package pe.sotomayor.creditos.security.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import org.springframework.http.MediaType;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.request.UpdateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.application.usecase.user.CreateUserUseCase;
import pe.sotomayor.creditos.security.application.usecase.user.DeactivateUserUseCase;
import pe.sotomayor.creditos.security.application.usecase.user.ListUsersUseCase;
import pe.sotomayor.creditos.security.application.usecase.user.UpdateUserUseCase;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;

import pe.sotomayor.creditos.security.infrastructure.security.config.SecurityConfig;
import pe.sotomayor.creditos.security.infrastructure.security.config.SecurityErrorHandler;
import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.GlobalExceptionHandler;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@WebMvcTest(controllers = UserController.class, properties = {
        "app.security.allowed-origins=http://localhost:5173", "app.security.docs-enabled=false"})
@Import({SecurityConfig.class, SecurityErrorHandler.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @MockitoBean
    private JwtService jwt;
    @MockitoBean
    private CustomUserDetailsService users;
    @MockitoBean
    private CreateUserUseCase create;
    @MockitoBean
    private UpdateUserUseCase update;
    @MockitoBean
    private ListUsersUseCase list;
    @MockitoBean
    private DeactivateUserUseCase deactivate;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateUserWhenPermissionIsGranted() throws Exception {
        /* Arrange */
        CreateUserRequest request = createCreateRequest();
        UserResponse expected = createResponse();
        when(create.create(request)).thenReturn(expected);

        /* Act */
        MvcResult result = mvc.perform(post("/api/security/users")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(json.readValue(result.getResponse().getContentAsString(), UserResponse.class)).isEqualTo(expected);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("password", "tokenVersion");
        verify(create).create(request);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldDenyCreateWhenAuthorityIsMissing() throws Exception {
        /* Arrange */
        CreateUserRequest request = createCreateRequest();

        /* Act */
        MvcResult result = mvc.perform(post("/api/security/users")
                .with(user("reader@test.com").authorities(new SimpleGrantedAuthority("security:users:read")))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("FORBIDDEN");
        verifyNoInteractions(create, update, list, deactivate, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldUpdateUserWhenPermissionIsGranted() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createUpdateRequest();
        UserResponse expected = createResponse();
        when(update.update(1L, request)).thenReturn(expected);

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/users/1")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readValue(result.getResponse().getContentAsString(), UserResponse.class)).isEqualTo(expected);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("password", "tokenVersion");
        verify(update).update(1L, request);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldDenyUpdateWhenAuthorityIsMissing() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createUpdateRequest();

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/users/1")
                .with(user("reader@test.com").authorities(new SimpleGrantedAuthority("security:users:read")))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("FORBIDDEN");
        verifyNoInteractions(create, update, list, deactivate, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldListUserWhenPermissionIsGranted() throws Exception {
        /* Arrange */
        UserResponse expected = createResponse();
        when(list.findAll()).thenReturn(List.of(expected));

        /* Act */
        MvcResult result = mvc.perform(get("/api/security/users")
                .with(user(createPrincipal()))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readTree(result.getResponse().getContentAsString()).size()).isEqualTo(1);
        assertThat(json.treeToValue(json.readTree(result.getResponse().getContentAsString()).get(0), UserResponse.class)).isEqualTo(expected);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("password", "tokenVersion");
        verify(list).findAll();
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldDenyListWhenAuthorityIsMissing() throws Exception {
        /* Arrange */
        String loginName = "reader@test.com";

        /* Act */
        MvcResult result = mvc.perform(get("/api/security/users")
                .with(user("reader@test.com").authorities(new SimpleGrantedAuthority("unrelated:read"))))
                .andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("FORBIDDEN");
        verifyNoInteractions(create, update, list, deactivate, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldDeactivateUserWhenPermissionIsGranted() throws Exception {
        /* Arrange */
        UserResponse expected = createResponse();
        when(deactivate.deactivate(1L)).thenReturn(expected);

        /* Act */
        MvcResult result = mvc.perform(patch("/api/security/users/1/deactivate")
                .with(user(createPrincipal()))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readValue(result.getResponse().getContentAsString(), UserResponse.class)).isEqualTo(expected);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("password", "tokenVersion");
        verify(deactivate).deactivate(1L);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldDenyDeactivateWhenAuthorityIsMissing() throws Exception {
        /* Arrange */
        String loginName = "reader@test.com";

        /* Act */
        MvcResult result = mvc.perform(patch("/api/security/users/1/deactivate")
                .with(user("reader@test.com").authorities(new SimpleGrantedAuthority("security:users:read"))))
                .andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("FORBIDDEN");
        verifyNoInteractions(create, update, list, deactivate, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldRequireAuthenticationWhenListing() throws Exception {
        /* Arrange */
        String endpoint = "/api/security/users";

        /* Act */
        MvcResult result = mvc.perform(get(endpoint)).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        verifyNoInteractions(create, update, list, deactivate, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldRejectPayloadWhenRequiredNameIsBlank() throws Exception {
        /* Arrange */
        CreateUserRequest valid = createCreateRequest();
        ObjectNode payload = json.valueToTree(valid);
        payload.put("fullName", "");

        /* Act */
        MvcResult result = mvc.perform(post("/api/security/users")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        verifyNoInteractions(create, update, list, deactivate, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldDenyCreationWhenAssignRolePermissionIsMissing() throws Exception {
        /* Arrange */
        CreateUserRequest request = createCreateRequest();

        /* Act */
        MvcResult result = mvc.perform(post("/api/security/users")
                .with(user("writer@test.com").authorities(new SimpleGrantedAuthority("security:users:write")))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        verifyNoInteractions(create, update, list, deactivate, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldRejectCreationWhenPasswordExceedsUtf8ByteLimit() throws Exception {
        /* Arrange */
        CreateUserRequest valid = createCreateRequest();
        CreateUserRequest request = new CreateUserRequest(valid.fullName(), valid.email(), "á".repeat(37), valid.roleId(), valid.branchId());

        /* Act */
        MvcResult result = mvc.perform(post("/api/security/users")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        verifyNoInteractions(create, update, list, deactivate, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldAcceptUpdateWithoutAssignRoleAuthorityWhenServiceAllowsIt() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createUpdateRequest();
        when(update.update(1L, request)).thenReturn(createResponse());

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/users/1")
                .with(user("writer@test.com").authorities(new SimpleGrantedAuthority("security:users:write")))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        verify(update).update(1L, request);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldReturnControlledErrorWhenRecordIsMissing() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createUpdateRequest();
        when(update.update(1L, request)).thenThrow(new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "missing"));

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/users/1")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("USER_NOT_FOUND");
        assertThat(body.path("message").asText()).doesNotContain("private SQL");
        verify(update).update(1L, request);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldReturnControlledErrorWhenVersionIsStale() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createUpdateRequest();
        when(update.update(1L, request)).thenThrow(new OptimisticLockingFailureException("private SQL"));

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/users/1")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(body.path("message").asText()).doesNotContain("private SQL");
        verify(update).update(1L, request);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldReturnControlledErrorWhenDatabaseConstraintFails() throws Exception {
        /* Arrange */
        UpdateUserRequest request = createUpdateRequest();
        when(update.update(1L, request)).thenThrow(new DataIntegrityViolationException("private SQL"));

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/users/1")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("DATA_INTEGRITY_CONFLICT");
        assertThat(body.path("message").asText()).doesNotContain("private SQL");
        verify(update).update(1L, request);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    private SecurityPrincipal createPrincipal() {
        return new SecurityPrincipal(
                1L,
                1L,
                "Ana Perez",
                "ana@test.com",
                "encoded-password",
                2L,
                "Operador",
                3L,
                "Lima",
                true,
                SecurityPermission.authorities().stream()
                        .map(SimpleGrantedAuthority::new).toList()
        );
    }

    private UserResponse createResponse() {
        return new UserResponse(
                1L,
                "Ana Perez",
                "ana@test.com",
                2L,
                "Operador",
                3L,
                "Lima",
                true
        );
    }

    private CreateUserRequest createCreateRequest() {
        return new CreateUserRequest(
                "Ana Perez",
                "ana@test.com",
                "a strong password",
                2L,
                3L
        );
    }

    private UpdateUserRequest createUpdateRequest() {
        return new UpdateUserRequest(
                "Ana Perez",
                "ana@test.com",
                2L,
                3L
        );
    }
}
