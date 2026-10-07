package pe.sotomayor.creditos.security.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

import pe.sotomayor.creditos.security.application.dto.role.request.CreateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRolePermissionsRequest;
import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;

import pe.sotomayor.creditos.security.application.usecase.role.CreateRoleUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.DeactivateRoleUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.GetRolePermissionsUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.ListRolesUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.UpdateRoleUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.UpdateRolePermissionsUseCase;

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
import java.util.Set;

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

@WebMvcTest(controllers = RoleController.class, properties = {
        "app.security.allowed-origins=http://localhost:5173", "app.security.docs-enabled=false"})
@Import({SecurityConfig.class, SecurityErrorHandler.class, GlobalExceptionHandler.class})
class RoleControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @MockitoBean
    private JwtService jwt;
    @MockitoBean
    private CustomUserDetailsService users;
    @MockitoBean
    private CreateRoleUseCase create;
    @MockitoBean
    private UpdateRoleUseCase update;
    @MockitoBean
    private ListRolesUseCase list;
    @MockitoBean
    private DeactivateRoleUseCase deactivate;
    @MockitoBean
    private GetRolePermissionsUseCase permissions;
    @MockitoBean
    private UpdateRolePermissionsUseCase setPermissions;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateRoleWhenPermissionIsGranted() throws Exception {
        /* Arrange */
        CreateRoleRequest request = createCreateRequest();
        RoleResponse expected = createResponse();
        when(create.create(request)).thenReturn(expected);

        /* Act */
        MvcResult result = mvc.perform(post("/api/security/roles")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(json.readValue(result.getResponse().getContentAsString(), RoleResponse.class)).isEqualTo(expected);
        verify(create).create(request);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldDenyCreateWhenAuthorityIsMissing() throws Exception {
        /* Arrange */
        CreateRoleRequest request = createCreateRequest();

        /* Act */
        MvcResult result = mvc.perform(post("/api/security/roles")
                .with(user("reader@test.com").authorities(new SimpleGrantedAuthority("security:roles:read")))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("FORBIDDEN");
        verifyNoInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldUpdateRoleWhenPermissionIsGranted() throws Exception {
        /* Arrange */
        UpdateRoleRequest request = createUpdateRequest();
        RoleResponse expected = createResponse();
        when(update.update(2L, request)).thenReturn(expected);

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/roles/2")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readValue(result.getResponse().getContentAsString(), RoleResponse.class)).isEqualTo(expected);
        verify(update).update(2L, request);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldDenyUpdateWhenAuthorityIsMissing() throws Exception {
        /* Arrange */
        UpdateRoleRequest request = createUpdateRequest();

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/roles/2")
                .with(user("reader@test.com").authorities(new SimpleGrantedAuthority("security:roles:read")))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("FORBIDDEN");
        verifyNoInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldListRoleWhenPermissionIsGranted() throws Exception {
        /* Arrange */
        RoleResponse expected = createResponse();
        when(list.findAll()).thenReturn(List.of(expected));

        /* Act */
        MvcResult result = mvc.perform(get("/api/security/roles").with(user(createPrincipal()))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readTree(result.getResponse().getContentAsString()).size()).isEqualTo(1);
        assertThat(json.treeToValue(json.readTree(result.getResponse().getContentAsString()).get(0), RoleResponse.class)).isEqualTo(expected);
        verify(list).findAll();
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldDenyListWhenAuthorityIsMissing() throws Exception {
        /* Arrange */
        String loginName = "reader@test.com";

        /* Act */
        MvcResult result = mvc.perform(get("/api/security/roles")
                        .with(user("reader@test.com")
                                .authorities(new SimpleGrantedAuthority("unrelated:read"))))
                .andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("FORBIDDEN");
        verifyNoInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldDeactivateRoleWhenPermissionIsGranted() throws Exception {
        /* Arrange */
        RoleResponse expected = createResponse();
        when(deactivate.deactivate(2L)).thenReturn(expected);

        /* Act */
        MvcResult result = mvc.perform(patch("/api/security/roles/2/deactivate")
                .with(user(createPrincipal()))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readValue(result.getResponse().getContentAsString(), RoleResponse.class)).isEqualTo(expected);
        verify(deactivate).deactivate(2L);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldDenyDeactivateWhenAuthorityIsMissing() throws Exception {
        /* Arrange */
        String loginName = "reader@test.com";

        /* Act */
        MvcResult result = mvc.perform(patch("/api/security/roles/2/deactivate")
                        .with(user("reader@test.com").authorities(new SimpleGrantedAuthority("security:roles:read"))))
                .andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("FORBIDDEN");
        verifyNoInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldRequireAuthenticationWhenListing() throws Exception {
        /* Arrange */
        String endpoint = "/api/security/roles";

        /* Act */
        MvcResult result = mvc.perform(get(endpoint)).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        verifyNoInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldRejectPayloadWhenRequiredNameIsBlank() throws Exception {
        /* Arrange */
        CreateRoleRequest valid = createCreateRequest();
        ObjectNode payload = json.valueToTree(valid);
        payload.put("name", "");

        /* Act */
        MvcResult result = mvc.perform(post("/api/security/roles")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        verifyNoInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldReturnControlledErrorWhenRecordIsMissing() throws Exception {
        /* Arrange */
        UpdateRoleRequest request = createUpdateRequest();
        when(update.update(2L, request)).thenThrow(new ResourceNotFoundException(ErrorCode.ROLE_NOT_FOUND, "missing"));

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/roles/2")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("ROLE_NOT_FOUND");
        assertThat(body.path("message").asText()).doesNotContain("private SQL");
        verify(update).update(2L, request);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldReturnControlledErrorWhenVersionIsStale() throws Exception {
        /* Arrange */
        UpdateRoleRequest request = createUpdateRequest();
        when(update.update(2L, request)).thenThrow(new OptimisticLockingFailureException("private SQL"));

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/roles/2")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(body.path("message").asText()).doesNotContain("private SQL");
        verify(update).update(2L, request);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldReturnControlledErrorWhenDatabaseConstraintFails() throws Exception {
        /* Arrange */
        UpdateRoleRequest request = createUpdateRequest();
        when(update.update(2L, request)).thenThrow(new DataIntegrityViolationException("private SQL"));

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/roles/2")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("DATA_INTEGRITY_CONFLICT");
        assertThat(body.path("message").asText()).doesNotContain("private SQL");
        verify(update).update(2L, request);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldReturnCatalogWhenReadPermissionIsGranted() throws Exception {
        /* Arrange */
        Set<String> expected = SecurityPermission.authorities();

        /* Act */
        MvcResult result = mvc.perform(get("/api/security/roles/permissions").with(user(createPrincipal()))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readValue(result.getResponse().getContentAsString(), String[].class)).containsExactlyElementsOf(expected.stream().sorted().toList());
        verifyNoInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldReturnRolePermissionsWhenReadPermissionIsGranted() throws Exception {
        /* Arrange */
        Set<String> expected = Set.of("security:users:read");
        when(permissions.getById(2L)).thenReturn(expected);

        /* Act */
        MvcResult result = mvc.perform(get("/api/security/roles/2/permissions").with(user(createPrincipal()))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readValue(result.getResponse().getContentAsString(), String[].class)).containsExactly("security:users:read");
        verify(permissions).getById(2L);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldReplacePermissionsWhenDedicatedAuthorityIsGranted() throws Exception {
        /* Arrange */
        UpdateRolePermissionsRequest request = createPermissionsRequest();
        when(setPermissions.update(2L, request)).thenReturn(request.permissions());

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/roles/2/permissions")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readValue(result.getResponse().getContentAsString(), String[].class)).containsExactly("security:users:read");
        verify(setPermissions).update(2L, request);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldDenyPermissionMutationWhenOnlyRoleWriteIsGranted() throws Exception {
        /* Arrange */
        UpdateRolePermissionsRequest request = createPermissionsRequest();

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/roles/2/permissions")
                .with(user("writer@test.com").authorities(new SimpleGrantedAuthority("security:roles:write")))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        verifyNoInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"permissions\":[\" \"]}"})
    void shouldRejectPermissionsWhenPayloadViolatesConstraints(String invalidJson) throws Exception {
        /* Arrange */
        String payload = invalidJson;

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/roles/2/permissions")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(payload)).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        verifyNoInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, permissions, setPermissions, jwt, users);
    }

    @Test
    void shouldAcceptEntireCurrentCatalog() throws Exception {
        /* Arrange */
        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest(SecurityPermission.authorities());
        when(setPermissions.update(2L, request)).thenReturn(request.permissions());

        /* Act */
        mvc.perform(put("/api/security/roles/2/permissions")
                        .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        /* Assert */
        verify(setPermissions).update(2L, request);
    }

    @Test
    void shouldRejectOversizedInputBeforeService() throws Exception {
        /* Arrange */
        Set<String> tooMany = java.util.stream.IntStream.range(0, 65)
                .mapToObj(i -> "permission-" + i).collect(java.util.stream.Collectors.toSet());

        /* Act */
        mvc.perform(put("/api/security/roles/2/permissions")
                        .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new UpdateRolePermissionsRequest(tooMany))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());

        /* Assert */
    }


    private UpdateRolePermissionsRequest createPermissionsRequest() {
        return new UpdateRolePermissionsRequest(Set.of("security:users:read"));
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

    private RoleResponse createResponse() {
        return new RoleResponse(2L, "Operador", "Consulta de usuarios");
    }

    private CreateRoleRequest createCreateRequest() {
        return new CreateRoleRequest("Operador", "Consulta de usuarios");
    }

    private UpdateRoleRequest createUpdateRequest() {
        return new UpdateRoleRequest("Operador", "Consulta de usuarios");
    }
}
