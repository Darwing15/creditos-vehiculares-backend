package pe.sotomayor.creditos.customers.integration;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pe.sotomayor.creditos.customers.application.mapper.CustomerMapperImpl;
import pe.sotomayor.creditos.customers.application.service.ListCustomersService;
import pe.sotomayor.creditos.customers.application.usecase.CreateCustomerUseCase;
import pe.sotomayor.creditos.customers.application.usecase.GetCustomerUseCase;
import pe.sotomayor.creditos.customers.application.usecase.UpdateCustomerUseCase;
import pe.sotomayor.creditos.customers.presentation.CustomerController;

import pe.sotomayor.creditos.shared.exception.GlobalExceptionHandler;
import pe.sotomayor.creditos.shared.exception.ValidationException;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.support.PostgresTestContainer;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestContainer.class, ListCustomersService.class,
        CustomerMapperImpl.class, CustomerPagePersistenceTest.MethodSecurity.class})
class CustomerPagePersistenceTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurity {
    }

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ListCustomersService service;
    @MockitoBean
    CurrentBranchProvider branchProvider;
    long branch;

    @BeforeEach
    void seed() {
        branch = jdbc.queryForObject("insert into branches (code,name,address,is_active) values ('PAGA','PAG A','DIR',true) returning id", Long.class);
        long other = jdbc.queryForObject("insert into branches (code,name,address,is_active) values ('PAGB','PAG B','DIR',true) returning id", Long.class);
        when(branchProvider.getCurrentBranchId()).thenReturn(branch);
        for (int i = 0; i < 12; i++)
            insert(branch, "Cliente " + String.format("%02d", i), "P" + i, i == 0 ? "998877" : null,
                    i == 0 ? "Avenida Perú 50%_\\zona" : null, true, false);
        insert(other, "Cliente SECRETO", "OTRO", null, "Avenida Perú", true, false);
        insert(branch, "Cliente inactivo", "INACT", null, null, false, false);
        insert(branch, "Cliente eliminado", "BORRADO", null, null, true, true);
    }

    void insert(long b, String name, String document, String phone, String address, boolean active, boolean deleted) {
        jdbc.update("""
                insert into customers(customer_code,full_name,document_type,document_number,phone,address,branch_id,is_active,is_deleted,deleted_at)
                values(gen_random_uuid(),?,'CE',?,?,?,?,?,?,case when ? then now() else null end)
                """, name, document, phone, address, b, active, deleted, deleted);
    }

    @Test
    @WithMockUser(authorities = "customers:read")
    void paginatesStablyAndEnforcesBranchActiveAndSoftDelete() {

        /* Arrange */

        int pageSize = 10; // seed() crea clientes activos, inactivos y de otra sucursal.

        /* Act */

        var first = service.searchPage("", 0, pageSize);
        var second = service.searchPage("", 1, pageSize);
        var foreign = service.searchPage("secreto", 0, pageSize);
        var inactive = service.searchPage("inactivo", 0, pageSize);
        var deleted = service.searchPage("eliminado", 0, pageSize);
        var legacy = service.listActive();
        var beyond = service.searchPage("", 3, pageSize);

        /* Assert */

        assertThat(first.items()).hasSize(10);
        assertThat(second.items()).hasSize(2);
        assertThat(first.totalElements()).isEqualTo(12);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.items()).extracting("id")
                .doesNotContainAnyElementsOf(second.items().stream()
                        .map(c -> c.id()).toList());
        assertThat(foreign.items()).isEmpty();
        assertThat(inactive.items()).isEmpty();
        assertThat(deleted.items()).isEmpty();
        assertThat(legacy).hasSize(12);
        assertThat(beyond.items()).isEmpty();
    }

    @Test
    @WithMockUser(authorities = "customers:read")
    void matchesNamesDocumentsPhonesAndAddressesWithLiteralCharacters() {

        /* Arrange */

        var terms = List.of("cliente 00", "p0", "9988", " avenida perú ", "50%_\\zona", "%", "_");

        /* Act */

        var matches = terms.stream().map(q -> service.searchPage(q, 0, 10)).toList();
        var injection = service.searchPage("' or 1=1 --", 0, 10);

        /* Assert */

        assertThat(matches).allSatisfy(result -> assertThat(result.items()).hasSize(1));
        assertThat(injection.items()).isEmpty();
    }

    @Test
    @WithMockUser(authorities = "customers:read")
    void rejectsInvalidParameters() {

        /* Arrange */

        int[][] invalid = {{-1, 10}, {1_000_001, 10}, {0, 0}, {0, 101}};

        /* Act */

        var failures = Arrays.stream(invalid).map(v -> catchThrowable(
                () -> service.searchPage("", v[0], v[1]))).toList();
        var longQuery = catchThrowable(
                () -> service.searchPage("x".repeat(151), 0, 10));

        /* Assert */

        assertThat(failures).allSatisfy(
                failure -> assertThat(failure)
                        .isInstanceOf(ValidationException.class));
        assertThat(longQuery).isInstanceOf(ValidationException.class);
    }

    @Test
    @WithMockUser(authorities = "vehicles:read")
    void deniesReadWithoutCustomerPermission() {

        /* Arrange */

        String query = "";

        /* Act */

        var failure = catchThrowable(() -> service.searchPage(query, 0, 10));

        /* Assert */

        assertThat(failure).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "customers:read")
    void httpContractUsesStableEnvelopeAndReturns400ForInvalidParameters() throws Exception {

        /* Arrange */

        var controller = new CustomerController(
                mock(CreateCustomerUseCase.class),
                mock(UpdateCustomerUseCase.class),
                mock(GetCustomerUseCase.class), service);
        var mvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var json = new ObjectMapper();

        /* Act */

        var success = mvc.perform(get("/api/customers/page")
                .param("q", "  avenida perú ")
                .param("page", "0")
                .param("size", "10"))
                .andReturn();
        var badSize = mvc.perform(get("/api/customers/page")
                .param("size", "101"))
                .andReturn();
        var badPage = mvc.perform(get("/api/customers/page")
                .param("page", "abc"))
                .andReturn();

        /* Assert */

        var body = json.readTree(success.getResponse().getContentAsString());
        assertThat(success.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.path("items").size()).isEqualTo(1);
        assertThat(body.path("totalElements").asInt()).isEqualTo(1);
        assertThat(body.path("page").asInt()).isZero();
        assertThat(badSize.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(badSize.getResponse().getContentAsString())
                .path("errorCode").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(badPage.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(badPage.getResponse().getContentAsString())
                .path("errorCode").asText()).isEqualTo("VALIDATION_ERROR");
    }
}
