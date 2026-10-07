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

import pe.sotomayor.creditos.security.application.dto.branch.request.CreateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.request.UpdateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

import pe.sotomayor.creditos.security.application.usecase.branch.CreateBranchUseCase;
import pe.sotomayor.creditos.security.application.usecase.branch.UpdateBranchUseCase;
import pe.sotomayor.creditos.security.application.usecase.branch.ListBranchesUseCase;
import pe.sotomayor.creditos.security.application.usecase.branch.DeactivateBranchUseCase;

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

@WebMvcTest(controllers = BranchController.class, properties = {
        "app.security.allowed-origins=http://localhost:5173", "app.security.docs-enabled=false"})
@Import({SecurityConfig.class, SecurityErrorHandler.class, GlobalExceptionHandler.class})
class BranchControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @MockitoBean
    private JwtService jwt;
    @MockitoBean
    private CustomUserDetailsService users;
    @MockitoBean
    private CreateBranchUseCase create;
    @MockitoBean
    private UpdateBranchUseCase update;
    @MockitoBean
    private ListBranchesUseCase list;
    @MockitoBean
    private DeactivateBranchUseCase deactivate;
    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void shouldCreateBranchWhenPermissionIsGranted() throws Exception {
        /* Arrange */
        CreateBranchRequest request = createCreateRequest();
        BranchResponse expected = createResponse();
        when(create.create(request)).thenReturn(expected);

        /* Act */
        MvcResult result = mvc.perform(post("/api/security/branches")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(json.readValue(result.getResponse().getContentAsString(), BranchResponse.class)).isEqualTo(expected);
        verify(create).create(request);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldDenyCreateWhenAuthorityIsMissing() throws Exception {
        /* Arrange */
        CreateBranchRequest request = createCreateRequest();

        /* Act */
        MvcResult result = mvc.perform(post("/api/security/branches")
                .with(user("reader@test.com").authorities(new SimpleGrantedAuthority("security:branches:read")))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("FORBIDDEN");
        verifyNoInteractions(create, update, list, deactivate, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldUpdateBranchWhenPermissionIsGranted() throws Exception {
        /* Arrange */
        UpdateBranchRequest request = createUpdateRequest();
        BranchResponse expected = createResponse();
        when(update.update(3L, request)).thenReturn(expected);

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/branches/3")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readValue(result.getResponse().getContentAsString(), BranchResponse.class)).isEqualTo(expected);
        verify(update).update(3L, request);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldDenyUpdateWhenAuthorityIsMissing() throws Exception {
        /* Arrange */
        UpdateBranchRequest request = createUpdateRequest();

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/branches/3")
                .with(user("reader@test.com").authorities(new SimpleGrantedAuthority("security:branches:read")))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("FORBIDDEN");
        verifyNoInteractions(create, update, list, deactivate, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldListBranchWhenPermissionIsGranted() throws Exception {
        /* Arrange */
        BranchResponse expected = createResponse();
        when(list.findAll()).thenReturn(List.of(expected));

        /* Act */
        MvcResult result = mvc.perform(get("/api/security/branches").with(user(createPrincipal()))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readTree(result.getResponse().getContentAsString()).size()).isEqualTo(1);
        assertThat(json.treeToValue(json.readTree(result.getResponse().getContentAsString()).get(0), BranchResponse.class)).isEqualTo(expected);
        verify(list).findAll();
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldDenyListWhenAuthorityIsMissing() throws Exception {
        /* Arrange */
        String loginName = "reader@test.com";

        /* Act */
        MvcResult result = mvc.perform(get("/api/security/branches")
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
    void shouldDeactivateBranchWhenPermissionIsGranted() throws Exception {
        /* Arrange */
        BranchResponse expected = createResponse();
        when(deactivate.deactivate(3L)).thenReturn(expected);

        /* Act */
        MvcResult result = mvc.perform(patch("/api/security/branches/3/deactivate")
                .with(user(createPrincipal()))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readValue(result.getResponse().getContentAsString(), BranchResponse.class)).isEqualTo(expected);
        verify(deactivate).deactivate(3L);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldDenyDeactivateWhenAuthorityIsMissing() throws Exception {
        /* Arrange */
        String loginName = "reader@test.com";

        /* Act */
        MvcResult result = mvc.perform(patch("/api/security/branches/3/deactivate")
                .with(user("reader@test.com").authorities(new SimpleGrantedAuthority("security:branches:read"))))
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
        String endpoint = "/api/security/branches";

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
        CreateBranchRequest valid = createCreateRequest();
        ObjectNode payload = json.valueToTree(valid);
        payload.put("name", "");

        /* Act */
        MvcResult result = mvc.perform(post("/api/security/branches")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        verifyNoInteractions(create, update, list, deactivate, jwt, users);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldReturnControlledErrorWhenRecordIsMissing() throws Exception {
        /* Arrange */
        UpdateBranchRequest request = createUpdateRequest();
        when(update.update(3L, request)).thenThrow(new ResourceNotFoundException(ErrorCode.BRANCH_NOT_FOUND, "missing"));

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/branches/3")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("BRANCH_NOT_FOUND");
        assertThat(body.path("message").asText()).doesNotContain("private SQL");
        verify(update).update(3L, request);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldReturnControlledErrorWhenVersionIsStale() throws Exception {
        /* Arrange */
        UpdateBranchRequest request = createUpdateRequest();
        when(update.update(3L, request)).thenThrow(new OptimisticLockingFailureException("private SQL"));

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/branches/3")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(body.path("message").asText()).doesNotContain("private SQL");
        verify(update).update(3L, request);
        verifyNoMoreInteractions(create, update, list, deactivate, jwt, users);
    }

    @Test
    void shouldReturnControlledErrorWhenDatabaseConstraintFails() throws Exception {
        /* Arrange */
        UpdateBranchRequest request = createUpdateRequest();
        when(update.update(3L, request)).thenThrow(new DataIntegrityViolationException("private SQL"));

        /* Act */
        MvcResult result = mvc.perform(put("/api/security/branches/3")
                .with(user(createPrincipal())).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo("DATA_INTEGRITY_CONFLICT");
        assertThat(body.path("message").asText()).doesNotContain("private SQL");
        verify(update).update(3L, request);
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
    private BranchResponse createResponse() {
        return new BranchResponse(
                3L,
                "LIM",
                "Lima",
                "Av. Peru 123",
                "999999999",
                "lima@test.com",
                null,
                true
        );
    }
    private CreateBranchRequest createCreateRequest() {
        return new CreateBranchRequest(
                "LIM",
                "Lima",
                "Av. Peru 123",
                "999999999",
                "lima@test.com",
                null
        );
    }
    private UpdateBranchRequest createUpdateRequest() {
        return new UpdateBranchRequest(
                "Lima",
                "Av. Peru 123",
                "999999999",
                "lima@test.com",
                null
        );
    }

}
