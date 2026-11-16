package pe.sotomayor.creditos.credit.integration;

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

import pe.sotomayor.creditos.credit.application.mapper.creditapplication.CreditApplicationMapperImpl;
import pe.sotomayor.creditos.credit.application.service.creditapplication.ListCreditApplicationsService;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.ApproveCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.CancelCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.CreateCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.GetCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.RejectCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditsimulation.SimulateCreditApplicationUseCase;

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;
import pe.sotomayor.creditos.credit.presentation.controller.CreditApplicationController;
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
@Import({PostgresTestContainer.class, ListCreditApplicationsService.class,
        CreditApplicationMapperImpl.class, CreditApplicationPagePersistenceTest.MethodSecurity.class})
class CreditApplicationPagePersistenceTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurity {
    }

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ListCreditApplicationsService service;
    @MockitoBean
    CurrentBranchProvider branchProvider;
    long branch, other, crossCustomer, crossVehicle;

    @BeforeEach
    void seed() {
        branch = jdbc.queryForObject("insert into branches(code,name,address,is_active) values ('CAPGA','A','DIR',true) returning id", Long.class);
        other = jdbc.queryForObject("insert into branches(code,name,address,is_active) values ('CAPGB','B','DIR',true) returning id", Long.class);
        when(branchProvider.getCurrentBranchId()).thenReturn(branch);
        for (int i = 0; i < 12; i++) {
            long c = customer(branch, "CLIENTE NOMBRE" + i, "CAP" + i, true, false);
            long v = vehicle(branch, "MODELO" + i, "CAP" + i, false);
            application(branch, c, v, "SOL-" + String.format("%02d", i), false);
        }
        crossCustomer = customer(other, "NOMBRE SECRETO", "OTRO", true, false);
        crossVehicle = vehicle(other, "VEHICULO SECRETO", "OTRO", false);
        application(other, crossCustomer, crossVehicle, "SOL-OTRA-SUC", false);
        /* La FK individual no impide referencias inconsistentes entre sucursales: no revelar nombres. */
        application(branch, crossCustomer, crossVehicle, "SOL-CRUZADA", false);
        long inactive = customer(branch, "NOMBRE INACTIVO", "INACT", false, false);
        long vInactive = vehicle(branch, "MODELO INACTIVO", "INACT", false);
        application(branch, inactive, vInactive, "SOL-INACTIVA", false);
        long deleted = customer(branch, "NOMBRE ELIMINADO", "BOR", true, true);
        long vDeleted = vehicle(branch, "MODELO ELIMINADO", "BOR", true);
        application(branch, deleted, vDeleted, "SOL-DATOS-ELIM", false);
        long c = customer(branch, "CLIENTE DESCARTADO", "DES", true, false);
        long v = vehicle(branch, "VEH DESCARTADO", "DES", false);
        application(branch, c, v, "SOL-ELIMINADA", true);
    }

    long customer(long b, String name, String document, boolean active, boolean deleted) {
        return jdbc.queryForObject("""
                insert into customers(customer_code,full_name,document_type,document_number,branch_id,is_active,is_deleted,deleted_at)
                values(gen_random_uuid(),?,'CE',?,?,?, ?,case when ? then now() else null end) returning id
                """, Long.class, name, document, b, active, deleted, deleted);
    }

    long vehicle(long b, String model, String suffix, boolean deleted) {
        return jdbc.queryForObject("""
                insert into vehicles(vehicle_code,vehicle_type,brand,model,manufacturing_year,color,chassis_number,engine_number,reference_price,branch_id,status,is_deleted,deleted_at)
                values(gen_random_uuid(),'MOTO','HONDA',?,2025,'ROJO',?,?,2000,?,'AVAILABLE',?,case when ? then now() else null end) returning id
                """, Long.class, model, "CH" + suffix, "MO" + suffix, b, deleted, deleted);
    }

    void application(long b, long c, long v, String number, boolean deleted) {
        jdbc.update("""
                insert into credit_applications(application_code,application_number,currency,vehicle_price,down_payment,financed_amount,compensatory_interest_rate,installment_count,customer_id,vehicle_id,branch_id,status,is_deleted,deleted_at)
                values(gen_random_uuid(),?,'PEN',2000,500,1500,1.50,6,?,?,?,'PENDING',?,case when ? then now() else null end)
                """, number, c, v, b, deleted, deleted);
    }

    @Test
    @WithMockUser(authorities = {"credit-applications:read", "customers:read", "vehicles:read"})
    void pagesWithinBranchWithoutDeletedAndResolvesOnlyVisibleNames() {

        /* Arrange */

        var status = CreditApplicationStatus.PENDING; // seed() incluye datos inactivos, eliminados y de otra sucursal.

        /* Act */

        var first = service.searchPage(status, "", 0, 10);
        var second = service.searchPage(status, "", 1, 10);
        var crossed = service.searchPage(status, "SOL-CRUZADA", 0, 10).items().getFirst();
        var foreign = service.searchPage(status, "secreto", 0, 10);
        var inactive = service.searchPage(status, "nombre inactivo", 0, 10);
        var deleted = service.searchPage(status, "nombre eliminado", 0, 10);
        var deletedApplication = service.searchPage(status, "veh descartado", 0, 10);
        var beyond = service.searchPage(status, "", 9, 10);
        var approved = service.searchPage(CreditApplicationStatus.APPROVED, "", 0, 10);

        /* Assert */

        assertThat(first.items()).hasSize(10);
        assertThat(second.items()).hasSize(5);
        assertThat(first.totalElements()).isEqualTo(15);
        assertThat(first.items().getFirst().customerName()).isEqualTo("CLIENTE NOMBRE0");
        assertThat(first.items().getFirst().vehicleName()).isEqualTo("MOTO HONDA MODELO0");
        assertThat(crossed.customerName()).isNull();
        assertThat(crossed.vehicleName()).isNull();
        assertThat(foreign.items()).isEmpty();
        assertThat(inactive.items()).isEmpty();
        assertThat(deleted.items()).isEmpty();
        assertThat(deletedApplication.items()).isEmpty();
        assertThat(beyond.items()).isEmpty();
        assertThat(approved.items()).isEmpty();
    }

    @Test
    @WithMockUser(authorities = {"credit-applications:read"})
    void noCustomerOrVehiclePermissionMeansNoNameOrNameSearch() {

        /* Arrange */

        var status = CreditApplicationStatus.PENDING;
        var known = service.searchPage(status, "SOL-00", 0, 10).items().getFirst();
        String customerFallback = "Cliente #" + known.application().customerId();

        /* Act */

        var result = service.searchPage(status, "SOL-00", 0, 10);
        var byCustomerName = service.searchPage(status, "NOMBRE0", 0, 10);
        var byVehicleName = service.searchPage(status, "MODELO0", 0, 10);
        var byId = service.searchPage(status, customerFallback, 0, 10);

        /* Assert */

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().customerName()).isNull();
        assertThat(result.items().getFirst().vehicleName()).isNull();
        assertThat(byCustomerName.totalElements()).isZero();
        assertThat(byVehicleName.totalElements()).isZero();
        assertThat(byId.totalElements()).isPositive();
    }

    @Test
    @WithMockUser(authorities = {"credit-applications:read", "customers:read"})
    void customerPermissionDoesNotRevealVehicleNames() {

        /* Arrange */

        var status = CreditApplicationStatus.PENDING;

        /* Act */

        var result = service.searchPage(status, "NOMBRE0", 0, 10);
        var vehicleSearch = service.searchPage(status, "MODELO0", 0, 10);

        /* Assert */

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().customerName()).isNotNull();
        assertThat(result.items().getFirst().vehicleName()).isNull();
        assertThat(vehicleSearch.totalElements()).isZero();
    }

    @Test
    @WithMockUser(authorities = {"credit-applications:read", "vehicles:read"})
    void vehiclePermissionDoesNotRevealCustomerNames() {

        /* Arrange */

        var status = CreditApplicationStatus.PENDING;

        /* Act */

        var result = service.searchPage(status, "MODELO0", 0, 10);
        var customerSearch = service.searchPage(status, "NOMBRE0", 0, 10);

        /* Assert */

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().customerName()).isNull();
        assertThat(result.items().getFirst().vehicleName()).isNotNull();
        assertThat(customerSearch.totalElements()).isZero();
    }

    @Test
    @WithMockUser(authorities = {"credit-applications:read", "customers:read", "vehicles:read"})
    void treatsSpecialCharactersLiterallyAndValidatesLimits() {

        /* Arrange */

        var status = CreditApplicationStatus.PENDING;
        var terms = List.of("%", "_", "' or 1=1 --");
        int[][] invalid = {{-1, 10}, {1_000_001, 10}, {0, 0}, {0, 101}};

        /* Act */

        var searches = terms.stream()
                .map(q -> service.searchPage(status, q, 0, 10))
                .toList();
        var failures = Arrays.stream(invalid)
                .map(v -> catchThrowable(() -> service.searchPage(status, "", v[0], v[1])))
                .toList();
        var longQuery = catchThrowable(() -> service.searchPage(status, "x".repeat(151), 0, 10));

        /* Assert */

        assertThat(searches).allSatisfy(result -> assertThat(result.totalElements())
                .isZero());
        assertThat(failures).allSatisfy(f -> assertThat(f)
                .isInstanceOf(ValidationException.class));
        assertThat(longQuery).isInstanceOf(ValidationException.class);
    }

    @Test
    @WithMockUser(authorities = "customers:read")
    void deniesUsersWithoutCreditRead() {

        /* Arrange */

        var status = CreditApplicationStatus.PENDING;

        /* Act */

        var failure = catchThrowable(() -> service.searchPage(status, "", 0, 10));

        /* Assert */

        assertThat(failure).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "credit-applications:read")
    void onlyReturnsTheRequestedStatusAndKeepsTheLegacyList() {

        /* Arrange */

        jdbc.update("update credit_applications set status='CANCELLED', resolved_at=now() where application_number='SOL-00'");

        /* Act */

        var pending = service.searchPage(CreditApplicationStatus.PENDING, "SOL-00", 0, 10);
        var cancelled = service.searchPage(CreditApplicationStatus.CANCELLED, "SOL-00", 0, 10);
        var legacy = service.listByStatus(CreditApplicationStatus.CANCELLED);

        /* Assert */

        assertThat(pending.items()).isEmpty();
        assertThat(cancelled.items()).hasSize(1);
        assertThat(legacy).hasSize(1);
    }

    @Test
    @WithMockUser(authorities = "credit-applications:read")
    void httpEndpointReturnsContractAndRejectsBadParameters() throws Exception {

        /* Arrange */

        var controller = new CreditApplicationController(
                mock(CreateCreditApplicationUseCase.class),
                mock(ApproveCreditApplicationUseCase.class),
                mock(RejectCreditApplicationUseCase.class),
                mock(CancelCreditApplicationUseCase.class),
                mock(GetCreditApplicationUseCase.class),
                service, mock(SimulateCreditApplicationUseCase.class));
        var mvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        var json = new ObjectMapper();

        /* Act */

        var success = mvc.perform(get("/api/credit-applications/page")
                        .param("status", "PENDING")
                        .param("q", "SOL-00"))
                .andReturn();
        var badSize = mvc.perform(get("/api/credit-applications/page")
                        .param("status", "PENDING")
                        .param("size", "101"))
                .andReturn();
        var badStatus = mvc.perform(get("/api/credit-applications/page")
                        .param("status", "INVALIDO"))
                .andReturn();

        /* Assert */

        var body = json.readTree(success.getResponse().getContentAsString());
        assertThat(success.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.path("totalElements").asInt()).isEqualTo(1);
        assertThat(body.path("items").get(0).path("application")
                .path("applicationNumber").asText()).isEqualTo("SOL-00");
        assertThat(body.path("items").get(0).path("customerName").isNull()).isTrue();
        assertThat(body.path("items").get(0).path("vehicleName").isNull()).isTrue();
        assertThat(badSize.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(badSize.getResponse().getContentAsString())
                .path("errorCode").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(badStatus.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(badStatus.getResponse().getContentAsString())
                .path("errorCode").asText()).isEqualTo("VALIDATION_ERROR");
    }
}
