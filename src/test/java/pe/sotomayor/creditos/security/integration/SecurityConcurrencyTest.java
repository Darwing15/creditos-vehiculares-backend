package pe.sotomayor.creditos.security.integration;

import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;
import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;

import pe.sotomayor.creditos.security.domain.repository.BranchRepository;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;

import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRolePermissionsRequest;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.UnauthorizedException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.sotomayor.creditos.security.support.SecurityIntegrationConfig;
import org.junit.jupiter.api.Tag;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.annotation.DirtiesContext;
import pe.sotomayor.creditos.security.application.usecase.user.DeactivateUserUseCase;
import pe.sotomayor.creditos.security.application.usecase.role.UpdateRolePermissionsUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ActiveProfiles("security-integration")
@Tag("integration")
@SpringBootTest(classes = SecurityIntegrationConfig.class, properties = {
        "spring.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.security.jwt.issuer=creditos-api", "spring.security.jwt.audience=creditos-web",
        "spring.security.jwt.expiration=600000", "spring.security.jwt.clock-skew-seconds=0",
        "app.security.allowed-origins=http://localhost:5173", "app.security.docs-enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SecurityConcurrencyTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private BranchRepository branches;

    @Autowired
    private DeactivateUserUseCase deactivate;

    @Autowired
    private UpdateRolePermissionsUseCase permissions;

    @Autowired
    private PlatformTransactionManager transactions;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldKeepOneAdminWhenTwoAdminsDeactivateEachOther() throws Exception {

        /* Arrange */

        List<SecurityPrincipal> actors = new TransactionTemplate(transactions)
                .execute(status -> {
            jdbc.update("UPDATE users SET is_active = FALSE");

            Branch branch = branches.saveAndFlush(createBranch());
            Role firstRole = roles.saveAndFlush(createRole("CONC-FIRST"));
            Role secondRole = roles.saveAndFlush(createRole("CONC-SECOND"));
            User first = users.saveAndFlush(createUser("first@test.com", firstRole, branch));
            User second = users.saveAndFlush(createUser("second@test.com", secondRole, branch));

            return List.of(createPrincipal(first), createPrincipal(second));
        });

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        /* Act */

        List<Throwable> outcomes;

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Throwable> first = executor.submit(() -> executeAs(actors.get(0), ready, start,
                    () -> deactivate.deactivate(actors.get(1).getUserId())));
            Future<Throwable> second = executor.submit(() -> executeAs(actors.get(1), ready, start,
                    () -> deactivate.deactivate(actors.get(0).getUserId())));
            boolean bothReady = ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            if (!bothReady) throw new IllegalStateException("Workers did not start");
            outcomes = Arrays.asList(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        }

        Long remaining = jdbc.queryForObject("""
                SELECT count(*) FROM users u JOIN roles r ON r.id = u.role_id
                JOIN branches b ON b.id = u.branch_id
                WHERE u.is_active AND NOT u.is_deleted AND NOT r.is_deleted
                  AND b.is_active AND NOT b.is_deleted
                  AND (SELECT count(*) FROM role_permissions rp WHERE rp.role_id = r.id) = ?
                """, Long.class, SecurityPermission.authorities().size());

        /* Assert */

        assertThat(outcomes.stream().filter(Objects::isNull).count())
                .isEqualTo(1);

        assertThat(outcomes.stream().filter(Objects::nonNull).toList())
                .singleElement()
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);

        assertThat(remaining).isEqualTo(1L);
    }

    @Test
    void shouldRollbackLastRevocationWhenTwoAdminsRemoveOwnPermissions() throws Exception {

        /* Arrange */

        List<SecurityPrincipal> actors = new TransactionTemplate(transactions)
                .execute(status -> {
            jdbc.update("UPDATE users SET is_active = FALSE");

            Branch branch = branches.saveAndFlush(createBranch());
            Role firstRole = roles.saveAndFlush(createRole("CONC-FIRST"));
            Role secondRole = roles.saveAndFlush(createRole("CONC-SECOND"));
            User first = users.saveAndFlush(createUser("first@test.com", firstRole, branch));
            User second = users.saveAndFlush(createUser("second@test.com", secondRole, branch));

            return List.of(createPrincipal(first), createPrincipal(second));
        });

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        /* Act */

        List<Throwable> outcomes;
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Throwable> first = executor.submit(() -> executeAs(actors.get(0), ready, start,
                    () -> permissions.update(actors.get(0).getRoleId(), createPermissionsRequest())));
            Future<Throwable> second = executor.submit(() -> executeAs(actors.get(1), ready, start,
                    () -> permissions.update(actors.get(1).getRoleId(), createPermissionsRequest())));
            boolean bothReady = ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            if (!bothReady) throw new IllegalStateException("Workers did not start");
            outcomes = Arrays.asList(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        }

        Long remaining = jdbc.queryForObject("""
                SELECT count(*) FROM users u JOIN roles r ON r.id = u.role_id
                JOIN branches b ON b.id = u.branch_id
                WHERE u.is_active AND NOT u.is_deleted AND NOT r.is_deleted
                  AND b.is_active AND NOT b.is_deleted
                  AND (SELECT count(*) FROM role_permissions rp WHERE rp.role_id = r.id) = ?
                """, Long.class, SecurityPermission.authorities().size());

        /* Assert */

        assertThat(outcomes.stream().filter(Objects::isNull).count())
                .isEqualTo(1);
        assertThat(outcomes.stream().filter(Objects::nonNull).toList())
                .singleElement()
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LAST_SECURITY_ADMIN_REQUIRED);

        assertThat(remaining).isEqualTo(1L);
    }

    private Throwable executeAs(SecurityPrincipal principal, CountDownLatch ready,
                                CountDownLatch start, Runnable action) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(principal));
        try {
            return catchThrowable(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                jdbc.execute("SET LOCAL lock_timeout = '10s'");
                action.run();
            }));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Branch createBranch() {
        return Branch.builder()
                .code("CONC-LIM")
                .name("Concurrency")
                .address("Av. Peru 123")
                .active(true)
                .deleted(false)
                .build();
    }

    private Role createRole(String name) {
        return Role.builder()
                .name(name)
                .description("Concurrent administrators")
                .deleted(false)
                .permissions(new HashSet<>(SecurityPermission.authorities()))
                .build();
    }

    private User createUser(String email, Role role, Branch branch) {
        return User.builder()
                .fullName("Admin Test")
                .email(email)
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
                SecurityPermission.authorities().stream()
                        .map(SimpleGrantedAuthority::new).toList()
        );
    }

    private Authentication createAuthentication(SecurityPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private UpdateRolePermissionsRequest createPermissionsRequest() {
        return new UpdateRolePermissionsRequest(Set.of());
    }

}

