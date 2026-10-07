package pe.sotomayor.creditos.security.domain.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.support.SecurityIntegrationConfig;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
@Transactional
class UserRepositoryTest {

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

    @Test
    void shouldFetchAuthenticationGraphWhenSearchingById() throws Exception {
        /* Arrange */
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = roles.saveAndFlush(createRole());
        User user = users.saveAndFlush(createUser(role, branch));
        Long id = user.getId();
        entityManager.clear();

        /* Act */
        User result = users.findForAuthenticationById(id).orElseThrow();
        entityManager.clear();

        /* Assert */
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getRole().getName()).isEqualTo("TEST-OPERADOR");
        assertThat(result.getBranch().getCode()).isEqualTo("TEST-LIM");
        assertThat(result.getRole().getPermissions()).containsExactly("security:users:read");
    }

    @Test
    void shouldFetchAuthenticationGraphWhenSearchingByCaseInsensitiveEmail() throws Exception {
        /* Arrange */
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = roles.saveAndFlush(createRole());
        User user = users.saveAndFlush(createUser(role, branch));
        Long id = user.getId();
        entityManager.clear();

        /* Act */
        User result = users.findForAuthenticationByEmail("SECURITY-TEST@TEST.COM").orElseThrow();
        entityManager.clear();

        /* Assert */
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getRole().getName()).isEqualTo("TEST-OPERADOR");
        assertThat(result.getBranch().getCode()).isEqualTo("TEST-LIM");
        assertThat(result.getRole().getPermissions()).containsExactly("security:users:read");
    }

    @Test
    void shouldRetainDisabledAndDeletedStateWhenLoadingForAuthentication() throws Exception {
        /* Arrange */
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = roles.saveAndFlush(createRole());
        User user = users.saveAndFlush(createUser(role, branch));
        Long id = user.getId();
        entityManager.clear();
        jdbc.update("UPDATE users SET is_active = FALSE, is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE id = ?", id);

        /* Act */
        User result = users.findForAuthenticationById(id).orElseThrow();

        /* Assert */
        assertThat(result.getActive()).isFalse();
        assertThat(result.getDeleted()).isTrue();
        assertThat(users.findByIdAndDeletedFalse(id)).isEmpty();
        assertThat(users.findAllByDeletedFalse()).extracting(User::getId).doesNotContain(id);
    }

    @Test
    void shouldReturnEmptyWhenAuthenticationIdentityDoesNotExist() throws Exception {
        /* Arrange */
        Long id = Long.MAX_VALUE;

        /* Act */
        Optional<User> byId = users.findForAuthenticationById(id);
        Optional<User> byEmail = users.findForAuthenticationByEmail("missing@test.com");

        /* Assert */
        assertThat(byId).isEmpty();
        assertThat(byEmail).isEmpty();
    }

    @Test
    void shouldRejectDuplicateWhenEmailDiffersOnlyByCase() throws Exception {
        /* Arrange */
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = roles.saveAndFlush(createRole());
        User user = users.saveAndFlush(createUser(role, branch));
        Long id = user.getId();
        entityManager.clear();
        User duplicate = createUser(role, branch);
        duplicate.setEmail("SECURITY-TEST@TEST.COM");

        /* Act */
        Throwable thrown = catchThrowable(() -> users.saveAndFlush(duplicate));

        /* Assert */
        assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPreserveHistoricalReferencesWhenUserIsDeleted() throws Exception {
        /* Arrange */
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = roles.saveAndFlush(createRole());
        User user = users.saveAndFlush(createUser(role, branch));
        Long id = user.getId();
        entityManager.clear();
        jdbc.update("UPDATE users SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE id = ?", id);

        /* Act */
        boolean roleInUse = users.existsByRoleId(role.getId());
        boolean branchInUse = users.existsByBranchId(branch.getId());

        /* Assert */
        assertThat(roleInUse).isTrue();
        assertThat(branchInUse).isTrue();
    }

    @Test
    void shouldRejectStaleWriteWhenUserVersionHasChanged() throws Exception {
        /* Arrange */
        Branch branch = branches.saveAndFlush(createBranch());
        Role role = roles.saveAndFlush(createRole());
        User user = users.saveAndFlush(createUser(role, branch));
        Long id = user.getId();
        entityManager.clear();
        User stale = users.findById(id).orElseThrow();
        entityManager.detach(stale);
        jdbc.update("UPDATE users SET token_version = token_version + 1, row_version = row_version + 1 WHERE id = ?", id);
        stale.setFullName("Stale update");

        /* Act */
        Throwable thrown = catchThrowable(() -> users.saveAndFlush(stale));

        /* Assert */
        assertThat(thrown).isInstanceOf(ObjectOptimisticLockingFailureException.class);
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

}

