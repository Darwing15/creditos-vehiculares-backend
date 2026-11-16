package pe.sotomayor.creditos.bootstrap;

import org.flywaydb.core.Flyway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.testcontainers.containers.PostgreSQLContainer;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@Testcontainers
class InitialAdminProvisionerTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    static final char[] TEST_SECRET = "only-isolated-test-pass".toCharArray();

    Connection connection() throws Exception {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @BeforeEach
    void freshDatabase() {
        var flyway = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void mainMigrationsDoNotCreateUsersRolesOrBranches() throws Exception {

        /* Arrange */

        /* freshDatabase() crea el esquema sin datos iniciales. */
        try (Connection c = connection(); Statement sql = c.createStatement()) {

            /* Act */

            long users, roles, branches;
            try (ResultSet row = sql.executeQuery("SELECT (SELECT count(*) FROM users), " +
                    "(SELECT count(*) FROM roles), (SELECT count(*) FROM branches)")) {
                if (!row.next())
                    throw new IllegalStateException("La consulta no devolvió una fila");
                users = row.getLong(1);
                roles = row.getLong(2);
                branches = row.getLong(3);
            }

            /* Assert */

            assertThat(users).isZero();
            assertThat(roles).isZero();
            assertThat(branches).isZero();
        }
    }

    @Test
    void provisionsCompleteAdministratorWithEncodedPassword() throws Exception {

        /* Arrange */

        try (Connection c = connection(); Statement sql = c.createStatement()) {
            String email, hash, branchCode;
            boolean active, secondRow;
            long version;
            int permissions;

            /* Act */

            InitialAdminProvisioner.create(c,
                    "cent",
                    "Sede de prueba",
                    "Dirección de prueba",
                    "Administradora de prueba",
                    "Admin@Example.Invalid",
                    TEST_SECRET);

            try (ResultSet row = sql.executeQuery("""
                    SELECT u.email, u.password, u.is_active, u.token_version, b.code,
                           (SELECT count(*) FROM role_permissions p WHERE p.role_id = r.id)
                    FROM users u JOIN roles r ON r.id=u.role_id JOIN branches b ON b.id=u.branch_id
                    """)) {
                if (!row.next())
                    throw new IllegalStateException("No se creó el administrador");
                email = row.getString(1);
                hash = row.getString(2);
                active = row.getBoolean(3);
                version = row.getLong(4);
                branchCode = row.getString(5);
                permissions = row.getInt(6);
                secondRow = row.next();
            }

            /* Assert */

            assertThat(email).isEqualTo("admin@example.invalid");
            assertThat(new BCryptPasswordEncoder(12)
                    .matches(new String(TEST_SECRET), hash)).isTrue();
            assertThat(hash).doesNotContain(new String(TEST_SECRET));
            assertThat(active).isTrue();
            assertThat(version).isEqualTo(1);
            assertThat(branchCode).isEqualTo("CENT");
            assertThat(permissions)
                    .isEqualTo(SecurityPermission.authorities().size());
            assertThat(secondRow).isFalse();
        }
    }

    @Test
    void neverProvisionsTwice() throws Exception {

        /* Arrange */

        try (Connection c = connection(); Statement sql = c.createStatement()) {
            InitialAdminProvisioner.create(c,
                    "cent",
                    "Sede de prueba",
                    "Dirección de prueba",
                    "Administradora",
                    "admin@example.invalid",
                    TEST_SECRET);

            /* Act */

            var failure = catchThrowable(() -> InitialAdminProvisioner.create(c,
                    "NEW",
                    "Otra",
                    "Dirección",
                    "Otro administrador",
                    "other@example.invalid",
                    TEST_SECRET));

            long count;
            try (ResultSet row = sql.executeQuery("SELECT count(*) FROM users")) {
                row.next();
                count = row.getLong(1);
            }

            /* Assert */

            assertThat(failure).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya contiene datos");
            assertThat(count).isEqualTo(1);
        }
    }

    @Test
    void rejectsInvalidValuesWithoutCreatingData() throws Exception {

        /* Arrange */

        try (Connection c = connection(); Statement sql = c.createStatement()) {

            /* Act */

            var failure = catchThrowable(() -> InitialAdminProvisioner.create(c,
                    "CENT",
                    "Sede",
                    "Dirección",
                    "Admin",
                    "not-an-email",
                    "short".toCharArray()));

            long count;
            try (ResultSet row = sql.executeQuery("SELECT count(*) FROM users")) {
                row.next();
                count = row.getLong(1);
            }

            /* Assert */

            assertThat(failure).isInstanceOf(IllegalArgumentException.class);
            assertThat(count).isZero();
        }
    }

    @Test
    void rollsBackBranchAndRoleIfPermissionInsertFails() throws Exception {

        /* Arrange */

        try (Connection c = connection(); Statement sql = c.createStatement()) {
            sql.execute("ALTER TABLE role_permissions DROP CONSTRAINT chk_role_permissions_catalog");
            sql.execute("ALTER TABLE role_permissions ADD CONSTRAINT temporary_test_constraint CHECK (permission = 'imposible')");

            /* Act */

            var failure = catchThrowable(() -> InitialAdminProvisioner.create(c,
                    "CENT",
                    "Sede",
                    "Dirección",
                    "Admin",
                    "admin@example.invalid",
                    TEST_SECRET));

            long roles, branches, users;
            try (ResultSet row = sql.executeQuery("SELECT (SELECT count(*) FROM roles), " +
                    "(SELECT count(*) FROM branches), (SELECT count(*) FROM users)")) {
                row.next();
                roles = row.getLong(1);
                branches = row.getLong(2);
                users = row.getLong(3);
            }

            /* Assert */

            assertThat(failure).isInstanceOf(Exception.class);
            assertThat(roles).isZero();
            assertThat(branches).isZero();
            assertThat(users).isZero();
        }
    }

    @Test
    void concurrentAttemptsLeaveExactlyOneAdministrator() throws Exception {

        /* Arrange */

        var start = new CountDownLatch(1);
        boolean first, second;
        long count;
        try (var pool = Executors.newFixedThreadPool(2)) {
            var attempts = IntStream.range(0, 2).mapToObj(i -> pool.submit(() -> {
                try (Connection c = connection()) {
                    if (!start.await(10, TimeUnit.SECONDS))
                        throw new IllegalStateException("Timeout de prueba");
                    InitialAdminProvisioner.create(c,
                            "CENT",
                            "Sede",
                            "Dirección",
                            "Admin",
                            "admin@example.invalid",
                            TEST_SECRET);
                    return true;
                } catch (IllegalStateException alreadyCreated) {
                    return false;
                }
            })).toList();

            /* Act */

            start.countDown();
            first = attempts.get(0).get(30, TimeUnit.SECONDS);
            second = attempts.get(1).get(30, TimeUnit.SECONDS);
        }

        try (Connection c = connection(); Statement sql = c.createStatement();
             ResultSet row = sql.executeQuery("SELECT count(*) FROM users")) {
            row.next();
            count = row.getLong(1);
        }

        /* Assert */

        assertThat(first ^ second).isTrue();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void rejectsCredentialsInJdbcUrl() {

        /* Arrange */

        String userInfo = "jdbc:postgresql://name:secret@localhost/db";
        String encodedPassword = "jdbc:postgresql://localhost/db?pass%77ord=abc";
        String safeUrl = "jdbc:postgresql://localhost:5432/db?sslmode=verify-full";

        /* Act */

        var first = catchThrowable(() -> InitialAdminBootstrap.checkJdbcUrl(userInfo));
        var second = catchThrowable(() -> InitialAdminBootstrap.checkJdbcUrl(encodedPassword));
        var valid = catchThrowable(() -> InitialAdminBootstrap.checkJdbcUrl(safeUrl));

        /* Assert */

        assertThat(first).isInstanceOf(IllegalArgumentException.class);
        assertThat(second).isInstanceOf(IllegalArgumentException.class);
        assertThat(valid).isNull();
    }
}
