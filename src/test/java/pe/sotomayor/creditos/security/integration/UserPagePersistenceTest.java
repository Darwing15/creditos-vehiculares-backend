package pe.sotomayor.creditos.security.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;

import pe.sotomayor.creditos.security.application.mapper.user.UserMapperImpl;
import pe.sotomayor.creditos.security.application.service.user.ListUsersService;
import pe.sotomayor.creditos.shared.exception.ValidationException;
import pe.sotomayor.creditos.support.PostgresTestContainer;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestContainer.class, ListUsersService.class, UserMapperImpl.class,
        UserPagePersistenceTest.MethodSecurity.class})
class UserPagePersistenceTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurity {
    }

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ListUsersService service;
    long roleA, roleB, branchA, branchB, unusedRole, unusedBranch;

    @BeforeEach
    void seed() {
        roleA = jdbc.queryForObject("insert into roles(name,description) values ('PAGE-ROL-A','Prueba') returning id", Long.class);
        roleB = jdbc.queryForObject("insert into roles(name,description) values ('PAGE-ROL-B','Prueba') returning id", Long.class);
        unusedRole = jdbc.queryForObject("insert into roles(name,description) values ('PAGE-SIN-USO','Prueba') returning id", Long.class);
        branchA = jdbc.queryForObject("insert into branches(code,name,address,is_active) values ('USPA','SUC A','DIR',true) returning id", Long.class);
        branchB = jdbc.queryForObject("insert into branches(code,name,address,is_active) values ('USPB','SUC B','DIR',true) returning id", Long.class);
        unusedBranch = jdbc.queryForObject("insert into branches(code,name,address,is_active) values ('USPC','SIN USO','DIR',true) returning id", Long.class);
        for (int i = 0; i < 12; i++)
            insert("TEST-PAGE " + i, "page" + i + "@example.invalid", i < 6 ? roleA : roleB,
                    i % 2 == 0 ? branchA : branchB, i != 0, false);
        insert("TEST-PAGE ELIMINADO", "eliminado@example.invalid", unusedRole, unusedBranch, true, true);
    }

    void insert(String name, String email, long role, long branch, boolean active, boolean deleted) {
        jdbc.update("""
                insert into users(full_name,email,password,role_id,branch_id,is_active,is_deleted,deleted_at)
                values(?,?,'hash-ficticio',?,?,?,?,case when ? then now() else null end)
                """, name, email, role, branch, active, deleted, deleted);
    }

    @Test
    @WithMockUser(authorities = "security:users:read")
    void listIsGlobalAcrossBranchesIncludesInactiveAndExcludesDeleted() {

        /* Arrange */

        String query = "TEST-PAGE";

        /* Act */

        var first = service.searchPage(query, null, null, 0, 10);
        var second = service.searchPage(query, null, null, 1, 10);
        var deleted = service.searchPage("ELIMINADO", null, null, 0, 10);

        /* Assert */

        assertThat(first.totalElements()).isEqualTo(12);
        assertThat(first.items()).hasSize(10);
        assertThat(second.items()).hasSize(2);
        assertThat(first.items()).extracting("branchId").contains(branchA, branchB);
        assertThat(first.items()).anyMatch(user -> !user.active());
        assertThat(first.items()).extracting("id")
                .doesNotContainAnyElementsOf(second.items().stream()
                        .map(user -> user.id()).toList());
        assertThat(deleted.items()).isEmpty();
    }

    @Test
    @WithMockUser(authorities = "security:users:read")
    void queryAndRoleAndBranchFiltersAreCombined() {

        /* Arrange */

        String query = "PAGE";

        /* Act */

        var byRoleAndBranch = service.searchPage(query, roleA, branchA, 0, 10);
        var byEmail = service.searchPage("page1@example.invalid", null, null, 0, 10);
        var byRoleName = service.searchPage("page-rol-b", null, null, 0, 10);
        var literal = service.searchPage("%_", null, null, 0, 10);

        /* Assert */

        assertThat(byRoleAndBranch.totalElements()).isEqualTo(3);
        assertThat(byRoleAndBranch.items())
                .allMatch(user -> user.roleId().equals(roleA) && user.branchId().equals(branchA));
        assertThat(byEmail.items()).hasSize(1);
        assertThat(byRoleName.totalElements()).isEqualTo(6);
        assertThat(literal.items()).isEmpty();
    }

    @Test
    @WithMockUser(authorities = "security:users:read")
    void optionsAreBasedOnVisibleUsersWithoutAdditionalCatalogPermissions() {

        /* Arrange */

        // seed() incluye rol y sucursal usados solo por un usuario eliminado.
        long excludedRole = unusedRole;
        long excludedBranch = unusedBranch;

        /* Act */

        var options = service.filterOptions();

        /* Assert */

        assertThat(options.roles()).extracting("id")
                .contains(roleA, roleB).doesNotContain(excludedRole);
        assertThat(options.branches()).extracting("id")
                .contains(branchA, branchB).doesNotContain(excludedBranch);
    }

    @Test
    @WithMockUser(authorities = "security:users:read")
    void invalidLimitsAreRejected() {

        /* Arrange */

        int[][] invalidPages = {{-1, 10}, {1_000_001, 10}, {0, 0}, {0, 101}};

        /* Act */

        var failures = Arrays.stream(invalidPages)
                .map(values -> catchThrowable(() -> service.searchPage("", null, null, values[0], values[1])))
                .toList();
        var invalidRole = catchThrowable(() -> service.searchPage("", 0L, null, 0, 10));
        var invalidBranch = catchThrowable(() -> service.searchPage("", null, -1L, 0, 10));
        var longQuery = catchThrowable(() -> service.searchPage("x".repeat(151), null, null, 0, 10));

        /* Assert */

        assertThat(failures).allSatisfy(failure -> assertThat(failure)
                .isInstanceOf(ValidationException.class));
        assertThat(invalidRole).isInstanceOf(ValidationException.class);
        assertThat(invalidBranch).isInstanceOf(ValidationException.class);
        assertThat(longQuery).isInstanceOf(ValidationException.class);
    }

    @Test
    @WithMockUser(authorities = "customers:read")
    void deniesBothOperationsWithoutUserReadPermission() {

        /* Arrange */

        String query = "";

        /* Act */

        var pageFailure = catchThrowable(() -> service.searchPage(query, null, null, 0, 10));
        var optionsFailure = catchThrowable(() -> service.filterOptions());

        /* Assert */

        assertThat(pageFailure).isInstanceOf(AccessDeniedException.class);
        assertThat(optionsFailure).isInstanceOf(AccessDeniedException.class);
    }
}
