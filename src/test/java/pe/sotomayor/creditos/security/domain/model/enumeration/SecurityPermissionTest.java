package pe.sotomayor.creditos.security.domain.model.enumeration;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class SecurityPermissionTest {

    @Test
    void shouldExposeExactImmutableCatalogWhenAuthoritiesAreRequested() throws Exception {
        /* Arrange */
        Set<String> expected = Set.of(
                "security:users:read",
                "security:users:write",
                "security:users:assign-role",
                "security:roles:read",
                "security:roles:write",
                "security:roles:manage-permissions",
                "security:branches:read",
                "security:branches:write",
                "customers:read",
                "customers:write",
                "vehicles:read",
                "vehicles:write",
                "credit-applications:read",
                "credit-applications:create",
                "credit-applications:resolve"
        );

        /* Act */
        Set<String> result = SecurityPermission.authorities();

        Throwable throwable = catchThrowable(
                () -> result.add("invented")
        );

        /* Assert */
        assertThat(result)
                .containsExactlyInAnyOrderElementsOf(expected);

        assertThat(throwable)
                .isInstanceOf(UnsupportedOperationException.class);
    }

}