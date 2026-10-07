package pe.sotomayor.creditos.security.integration;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.auth.request.ChangePasswordRequest;
import pe.sotomayor.creditos.security.application.dto.auth.request.LoginRequest;
import pe.sotomayor.creditos.security.application.dto.role.request.CreateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRolePermissionsRequest;
import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;
import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;

import pe.sotomayor.creditos.security.domain.repository.BranchRepository;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.support.SecurityIntegrationConfig;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;


@ActiveProfiles("security-integration")
@Tag("integration")
@SpringBootTest(classes = SecurityIntegrationConfig.class, properties = {
        "spring.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.security.jwt.issuer=creditos-api", "spring.security.jwt.audience=creditos-web",
        "spring.security.jwt.expiration=600000", "spring.security.jwt.clock-skew-seconds=0",
        "app.security.allowed-origins=http://localhost:5173", "app.security.docs-enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"})
@AutoConfigureMockMvc
@Transactional
class SecurityFlowTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private UserRepository users;
    @Autowired
    private RoleRepository roles;
    @Autowired
    private BranchRepository branches;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldInvalidateBothSessionsWhenPasswordChanges() throws Exception {
        /* Arrange */
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = roles.saveAndFlush(createRole());
        User account = users.saveAndFlush(createUser(role, branch));
        String first = login("initial strong password");
        String second = login("initial strong password");
        ChangePasswordRequest request = createChangeRequest();

        /* Act */
        MvcResult mutation = mvc.perform(post("/api/auth/change-password")
                .header("Authorization", "Bearer " + first).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();
        MvcResult oldFirst = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + first)).andReturn();
        MvcResult oldSecond = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + second)).andReturn();
        String fresh = login("new strong password");
        MvcResult current = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + fresh)).andReturn();

        /* Assert */
        assertThat(mutation.getResponse().getStatus()).isEqualTo(204);
        assertThat(oldFirst.getResponse().getStatus()).isEqualTo(401);
        assertThat(oldSecond.getResponse().getStatus()).isEqualTo(401);
        assertThat(current.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readTree(current.getResponse().getContentAsString()).path("id").asLong()).isEqualTo(account.getId());
        entityManager.clear();
        assertThat(users.findForAuthenticationById(account.getId()).orElseThrow().getTokenVersion()).isEqualTo(2L);
    }

    @Test
    void shouldInvalidateBothSessionsWhenLogoutAllCompletes() throws Exception {
        /* Arrange */
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = roles.saveAndFlush(createRole());
        User account = users.saveAndFlush(createUser(role, branch));
        String first = login("initial strong password");
        String second = login("initial strong password");

        /* Act */
        MvcResult mutation = mvc.perform(post("/api/auth/logout-all")
                .header("Authorization", "Bearer " + first)).andReturn();
        MvcResult oldFirst = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + first)).andReturn();
        MvcResult oldSecond = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + second)).andReturn();
        String fresh = login("initial strong password");
        MvcResult current = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + fresh)).andReturn();

        /* Assert */
        assertThat(mutation.getResponse().getStatus()).isEqualTo(204);
        assertThat(oldFirst.getResponse().getStatus()).isEqualTo(401);
        assertThat(oldSecond.getResponse().getStatus()).isEqualTo(401);
        assertThat(current.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readTree(current.getResponse().getContentAsString()).path("id").asLong()).isEqualTo(account.getId());
        entityManager.clear();
        assertThat(users.findForAuthenticationById(account.getId()).orElseThrow().getTokenVersion()).isEqualTo(2L);
    }

    @Test
    void shouldPersistEncodedCredentialsWhenAuthorizedAdminCreatesUser() throws Exception {
        /* Arrange */
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = roles.saveAndFlush(createRole());
        User account = users.saveAndFlush(createUser(role, branch));
        String token = login("initial strong password");
        CreateUserRequest request = createUserRequest(role.getId(), branch.getId());

        /* Act */
        MvcResult result = mvc.perform(post("/api/security/users").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andReturn();
        entityManager.clear();
        User stored = users.findForAuthenticationByEmail(request.email()).orElseThrow();

        /* Assert */
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(stored.getTokenVersion()).isEqualTo(1L);
        assertThat(stored.getPassword()).isNotEqualTo(request.password());
        assertThat(encoder.matches(request.password(), stored.getPassword())).isTrue();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("password", "tokenVersion", stored.getPassword());
    }

    @Test
    void shouldApplyRevokedPermissionsWhenExistingTokenIsReused() throws Exception {
        /* Arrange */
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = roles.saveAndFlush(createRole());
        User account = users.saveAndFlush(createUser(role, branch));
        String token = login("initial strong password");
        UpdateRolePermissionsRequest request = createPermissionsRequest();

        /* Act */
        MvcResult changed = mvc.perform(put("/api/security/roles/" + role.getId() + "/permissions")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();
        MvcResult write = mvc.perform(post("/api/security/roles").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(createRoleRequest()))).andReturn();
        MvcResult read = mvc.perform(get("/api/security/users").header("Authorization", "Bearer " + token)).andReturn();

        /* Assert */
        assertThat(changed.getResponse().getStatus()).isEqualTo(200);
        assertThat(write.getResponse().getStatus()).isEqualTo(403);
        assertThat(read.getResponse().getStatus()).isEqualTo(200);
    }

    private String login(String password) throws Exception {
        LoginRequest request = new LoginRequest("flow@test.com", password);
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request))).andReturn();
        assertThat(result.getResponse().getStatus()).as("login setup").isEqualTo(200);
        return json.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private Branch createBranch() {
        return Branch.builder()
                .code("FLOW-LIM")
                .name("Lima Flow")
                .address("Av. Peru 123")
                .active(true)
                .deleted(false)
                .build();
    }

    private Role createRole() {
        return Role.builder()
                .name("FLOW-ADMIN")
                .description("Flow tests")
                .deleted(false)
                .permissions(new HashSet<>(SecurityPermission.authorities()))
                .build();
    }

    private User createUser(Role role, Branch branch) {
        return User.builder()
                .fullName("Ana Perez")
                .email("flow@test.com")
                .password(encoder.encode("initial strong password"))
                .role(role)
                .branch(branch)
                .active(true)
                .deleted(false)
                .tokenVersion(1L)
                .build();
    }

    private ChangePasswordRequest createChangeRequest() {
        return new ChangePasswordRequest("initial strong password", "new strong password");
    }

    private CreateUserRequest createUserRequest(Long roleId, Long branchId) {
        return new CreateUserRequest(
                "Nuevo Usuario",
                "created@test.com",
                "a new strong password",
                roleId,
                branchId
        );
    }

    private UpdateRolePermissionsRequest createPermissionsRequest() {
        return new UpdateRolePermissionsRequest(Set.of("security:users:read"));
    }

    private CreateRoleRequest createRoleRequest() {
        return new CreateRoleRequest("Unpermitted", "Attempt after permission removal");
    }

}
