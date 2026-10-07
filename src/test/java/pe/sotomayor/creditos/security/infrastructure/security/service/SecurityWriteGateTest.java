package pe.sotomayor.creditos.security.infrastructure.security.service;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.test.context.ActiveProfiles;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;

import pe.sotomayor.creditos.security.domain.repository.BranchRepository;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;
import pe.sotomayor.creditos.security.support.SecurityIntegrationConfig;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.UnauthorizedException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission.USERS_WRITE;
import static pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission.ROLES_MANAGE_PERMISSIONS;
import static pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission.ROLES_WRITE;

@ActiveProfiles("security-integration")
@Tag("integration")
@SpringBootTest(classes = SecurityIntegrationConfig.class, properties = {
        "spring.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.security.jwt.issuer=creditos-api", "spring.security.jwt.audience=creditos-web",
        "spring.security.jwt.expiration=600000", "spring.security.jwt.clock-skew-seconds=0",
        "app.security.allowed-origins=http://localhost:5173", "app.security.docs-enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"})
@Transactional
class SecurityWriteGateTest {
    @Autowired
    private UserRepository users;
    @Autowired
    private RoleRepository roles;
    @Autowired
    private BranchRepository branches;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private SecurityWriteGate gate;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentDatabasePermissionsWhenSessionIsValid() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = createRole();
        role.setPermissions(new HashSet<>(SecurityPermission.authorities()));
        role = roles.saveAndFlush(role);
        User user = users.saveAndFlush(createUser(role, branch));
        SecurityPrincipal principal = createPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(principal));

        /* Act */
        SecurityWriteGate.WriteContext result = gate.begin(USERS_WRITE, ROLES_WRITE);

        /* Assert */
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.permissions()).containsExactlyInAnyOrderElementsOf(SecurityPermission.authorities());
    }

    @Test
    void shouldRejectSessionWhenUserIsRevoked() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = createRole();
        role.setPermissions(new HashSet<>(SecurityPermission.authorities()));
        role = roles.saveAndFlush(role);
        User user = users.saveAndFlush(createUser(role, branch));
        SecurityPrincipal principal = createPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(principal));
        jdbc.update("UPDATE users SET token_version = token_version + 1 WHERE id = ?", user.getId());

        /* Act */
        Throwable thrown = catchThrowable(() -> gate.begin(USERS_WRITE));

        /* Assert */
        assertThat(thrown).isInstanceOf(UnauthorizedException.class).extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void shouldRejectSessionWhenUserIsInactive() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = createRole();
        role.setPermissions(new HashSet<>(SecurityPermission.authorities()));
        role = roles.saveAndFlush(role);
        User user = users.saveAndFlush(createUser(role, branch));
        SecurityPrincipal principal = createPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(principal));
        jdbc.update("UPDATE users SET is_active = FALSE WHERE id = ?", user.getId());

        /* Act */
        Throwable thrown = catchThrowable(() -> gate.begin(USERS_WRITE));

        /* Assert */
        assertThat(thrown).isInstanceOf(UnauthorizedException.class).extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void shouldRejectSessionWhenUserIsDeleted() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = createRole();
        role.setPermissions(new HashSet<>(SecurityPermission.authorities()));
        role = roles.saveAndFlush(role);
        User user = users.saveAndFlush(createUser(role, branch));
        SecurityPrincipal principal = createPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(principal));
        jdbc.update("UPDATE users SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE id = ?", user.getId());

        /* Act */
        Throwable thrown = catchThrowable(() -> gate.begin(USERS_WRITE));

        /* Assert */
        assertThat(thrown).isInstanceOf(UnauthorizedException.class).extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void shouldRejectSessionWhenRoleIsDeleted() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = createRole();
        role.setPermissions(new HashSet<>(SecurityPermission.authorities()));
        role = roles.saveAndFlush(role);
        User user = users.saveAndFlush(createUser(role, branch));
        SecurityPrincipal principal = createPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(principal));
        jdbc.update("UPDATE roles SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE id = ?", role.getId());

        /* Act */
        Throwable thrown = catchThrowable(() -> gate.begin(USERS_WRITE));

        /* Assert */
        assertThat(thrown).isInstanceOf(UnauthorizedException.class).extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void shouldRejectSessionWhenBranchIsDeleted() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = createRole();
        role.setPermissions(new HashSet<>(SecurityPermission.authorities()));
        role = roles.saveAndFlush(role);
        User user = users.saveAndFlush(createUser(role, branch));
        SecurityPrincipal principal = createPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(principal));
        jdbc.update("UPDATE branches SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE id = ?", branch.getId());

        /* Act */
        Throwable thrown = catchThrowable(() -> gate.begin(USERS_WRITE));

        /* Assert */
        assertThat(thrown).isInstanceOf(UnauthorizedException.class).extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void shouldRejectWriteWhenPermissionWasRevokedAfterAuthentication() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = createRole();
        role.setPermissions(new HashSet<>(SecurityPermission.authorities()));
        role = roles.saveAndFlush(role);
        User user = users.saveAndFlush(createUser(role, branch));
        SecurityPrincipal principal = createPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(principal));
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ? AND permission = ?", role.getId(), "security:users:write");

        /* Act */
        Throwable thrown = catchThrowable(() -> gate.begin(USERS_WRITE));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldRejectWriteWhenAuthenticationIsAbsent() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();

        /* Act */
        Throwable thrown = catchThrowable(() -> gate.begin(USERS_WRITE));

        /* Assert */
        assertThat(thrown).isInstanceOf(UnauthorizedException.class).extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @Transactional(readOnly = true)
    void shouldRejectWriteWhenTransactionIsReadOnly() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();

        /* Act */
        Throwable thrown = catchThrowable(() -> gate.begin(USERS_WRITE));

        /* Assert */
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectFinishWhenLastAdminLosesPermissions() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = createRole();
        role.setPermissions(new HashSet<>(SecurityPermission.authorities()));
        role = roles.saveAndFlush(role);
        User user = users.saveAndFlush(createUser(role, branch));
        SecurityPrincipal principal = createPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(principal));
        jdbc.update("UPDATE users SET is_active = FALSE WHERE id <> ?", user.getId());
        gate.begin(ROLES_MANAGE_PERMISSIONS);
        role.getPermissions().clear();

        /* Act */
        Throwable thrown = catchThrowable(() -> gate.finish());

        /* Assert */
        assertThat(thrown).isInstanceOf(ConflictException.class).extracting("errorCode").isEqualTo(ErrorCode.LAST_SECURITY_ADMIN_REQUIRED);
    }

    @Test
    void shouldAcceptFinishWhenAnOperationalAdminRemains() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = createRole();
        role.setPermissions(new HashSet<>(SecurityPermission.authorities()));
        role = roles.saveAndFlush(role);
        User user = users.saveAndFlush(createUser(role, branch));
        SecurityPrincipal principal = createPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(principal));

        /* Act */
        gate.begin(USERS_WRITE);
        gate.finish();

        /* Assert */
        assertThat(users.findForAuthenticationById(user.getId())).isPresent();
    }

    @Test
    void shouldRejectPrivilegeEscalationWhenRequestedSetExceedsContext() throws Exception {
        /* Arrange */
        SecurityWriteGate.WriteContext context = createContext();

        /* Act */
        Throwable thrown = catchThrowable(() -> context.requireSubset(Set.of("security:roles:write")));

        /* Assert */
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
    }

    private Branch createBranch() {
        return Branch.builder()
                .code("TEST-LIM")
                .name("Lima Test")
                .address("Av. Peru 123")
                .active(true)
                .deleted(false)
                .build();
    }

    private Role createRole() {
        return Role.builder()
                .name("TEST-OPERADOR")
                .description("Pruebas")
                .permissions(new HashSet<>(Set.of("security:users:read")))
                .deleted(false)
                .build();
    }

    private User createUser(Role role, Branch branch) {
        return User.builder()
                .fullName("Ana Perez")
                .email("security-test@test.com")
                .password("encoded-password")
                .role(role)
                .branch(branch)
                .active(true)
                .deleted(false)
                .tokenVersion(1L)
                .build();
    }

    private SecurityPrincipal createPrincipal(User user) {
        return new SecurityPrincipal(
                user.getId(),
                user.getTokenVersion(),
                user.getFullName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole().getId(),
                user.getRole().getName(),
                user.getBranch().getId(),
                user.getBranch().getName(),
                true,
                List.of()
        );
    }

    private Authentication createAuthentication(SecurityPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private SecurityWriteGate.WriteContext createContext() {
        return new SecurityWriteGate.WriteContext(1L, Set.of("security:users:read"));
    }
}

