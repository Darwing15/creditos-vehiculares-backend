package pe.sotomayor.creditos.vehicles.integration;

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

import pe.sotomayor.creditos.shared.exception.GlobalExceptionHandler;
import pe.sotomayor.creditos.shared.exception.ValidationException;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import pe.sotomayor.creditos.support.PostgresTestContainer;

import pe.sotomayor.creditos.vehicles.application.mapper.VehicleMapperImpl;
import pe.sotomayor.creditos.vehicles.application.service.ListVehiclesService;
import pe.sotomayor.creditos.vehicles.application.usecase.CreateVehicleUseCase;
import pe.sotomayor.creditos.vehicles.application.usecase.GetVehicleUseCase;
import pe.sotomayor.creditos.vehicles.application.usecase.UpdateVehicleUseCase;
import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;
import pe.sotomayor.creditos.vehicles.presentation.VehicleController;

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
@Import({PostgresTestContainer.class, ListVehiclesService.class, VehicleMapperImpl.class, VehiclePagePersistenceTest.MethodSecurity.class})
class VehiclePagePersistenceTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurity {
    }

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ListVehiclesService service;
    @MockitoBean
    CurrentBranchProvider branchProvider;
    long branch;

    @BeforeEach
    void seed() {
        branch = jdbc.queryForObject("insert into branches(code,name,address,is_active) values ('VPAGA','A','DIR',true) returning id", Long.class);
        long other = jdbc.queryForObject("insert into branches(code,name,address,is_active) values ('VPAGB','B','DIR',true) returning id", Long.class);
        when(branchProvider.getCurrentBranchId()).thenReturn(branch);
        for (int i = 0; i < 12; i++)
            insert(branch, "HONDA", "M" + i, i == 0 ? "VIN%_\\ABC" : "VIN" + i,
                    "MOTOR" + i, i < 6 ? "AVAILABLE" : i < 10 ? "RESERVED" : "SOLD", false);
        insert(branch, "YAMAHA", "FZ", "VINY", "MOTORY", "AVAILABLE", false);
        insert(other, "SECRETO", "OTRO", "VINX", "MOTORX", "AVAILABLE", false);
        insert(branch, "BORRADO", "OTRO", "VINZ", "MOTORZ", "RESERVED", true);
    }

    void insert(long b, String brand, String model, String chassis, String engine, String status, boolean deleted) {
        jdbc.update("""
                insert into vehicles(vehicle_code,vehicle_type,brand,model,manufacturing_year,color,chassis_number,engine_number,reference_price,branch_id,status,is_deleted,deleted_at)
                values(gen_random_uuid(),'MOTO',?,?,2025,'ROJO',?,?,1000,?,cast(? as vehicle_status),?,case when ? then now() else null end)
                """, brand, model, chassis, engine, b, status, deleted, deleted);
    }

    @Test
    @WithMockUser(authorities = "vehicles:read")
    void pagesWithinBranchAndMaintainsUnfilteredOverview() {

        /* Arrange */

        int pageSize = 10; // seed() incluye otras sucursales y una unidad eliminada.

        /* Act */

        var first = service.searchPage("", null, "", 0, pageSize);
        var second = service.searchPage("", null, "", 1, pageSize);
        var hondaReserved = service.searchPage("", VehicleStatus.RESERVED, "HONDA", 0, pageSize);
        var yamahaReserved = service.searchPage("", VehicleStatus.RESERVED, "YAMAHA", 0, pageSize);
        var foreign = service.searchPage("secreto", null, "", 0, pageSize);
        var deleted = service.searchPage("borrado", null, "", 0, pageSize);
        var beyond = service.searchPage("", null, "", 3, pageSize);
        var summary = service.overview();
        var legacyAll = service.list(null);
        var legacyReserved = service.list(VehicleStatus.RESERVED);

        /* Assert */

        assertThat(first.items()).hasSize(10);
        assertThat(second.items()).hasSize(3);
        assertThat(first.totalElements()).isEqualTo(13);
        assertThat(first.items()).extracting("id")
                .doesNotContainAnyElementsOf(second.items().stream().map(v -> v.id()).toList());
        assertThat(hondaReserved.totalElements()).isEqualTo(4);
        assertThat(yamahaReserved.totalElements()).isZero();
        assertThat(foreign.totalElements()).isZero();
        assertThat(deleted.totalElements()).isZero();
        assertThat(beyond.items()).isEmpty();
        assertThat(summary.total()).isEqualTo(13);
        assertThat(summary.available()).isEqualTo(7);
        assertThat(summary.reserved()).isEqualTo(4);
        assertThat(summary.brands()).containsExactlyInAnyOrder("HONDA", "YAMAHA");
        assertThat(legacyAll).hasSize(13);
        assertThat(legacyReserved).hasSize(4);
    }

    @Test
    @WithMockUser(authorities = "vehicles:read")
    void searchesLiteralIdentifiersAndUuid() {

        /* Arrange */

        var terms = List.of(" honda m0 ", "vin%_\\abc", "motor0", "%", "_");
        String uuid = service.searchPage("", null, "", 0, 1)
                .items().getFirst().vehicleCode().toString();

        /* Act */

        var matches = terms.stream().map(
                q -> service.searchPage(q, null, "", 0, 10)).toList();
        var byUuid = service.searchPage(uuid, null, "", 0, 10);
        var injection = service.searchPage("' or 1=1 --", null, "", 0, 10);

        /* Assert */

        assertThat(matches).allSatisfy(
                result -> assertThat(result.items()).hasSize(1));
        assertThat(byUuid.items()).hasSize(1);
        assertThat(injection.items()).isEmpty();
    }

    @Test
    @WithMockUser(authorities = "vehicles:read")
    void rejectsInvalidLimits() {

        /* Arrange */

        int[][] invalid = {{-1, 10}, {1_000_001, 10}, {0, 0}, {0, 101}};

        /* Act */

        var failures = Arrays.stream(invalid).map(
                v -> catchThrowable(() -> service.searchPage("", null, "", v[0], v[1]))).toList();
        var longQuery = catchThrowable(() -> service.searchPage("x".repeat(151), null, "", 0, 10));
        var longBrand = catchThrowable(() -> service.searchPage("", null, "x".repeat(101), 0, 10));

        /* Assert */

        assertThat(failures).allSatisfy(
                f -> assertThat(f).isInstanceOf(ValidationException.class));
        assertThat(longQuery).isInstanceOf(ValidationException.class);
        assertThat(longBrand).isInstanceOf(ValidationException.class);
    }

    @Test
    @WithMockUser(authorities = "customers:read")
    void deniesUnauthorizedAccess() {

        /* Arrange */

        String query = "";

        /* Act */

        var pageFailure = catchThrowable(() -> service.searchPage(query, null, "", 0, 10));
        var overviewFailure = catchThrowable(() -> service.overview());

        /* Assert */
        assertThat(pageFailure).isInstanceOf(AccessDeniedException.class);
        assertThat(overviewFailure).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "vehicles:read")
    void emptyBranchReturnsZeroTotalsAndNoBrands() {

        /* Arrange */

        long emptyBranch = jdbc.queryForObject("insert into branches(code,name,address,is_active) values ('VPAGC','C','DIR',true) returning id", Long.class);
        when(branchProvider.getCurrentBranchId()).thenReturn(emptyBranch);

        /* Act */

        var summary = service.overview();
        var page = service.searchPage("", null, "", 0, 10);

        /* Assert */

        assertThat(summary.total()).isZero();
        assertThat(summary.available()).isZero();
        assertThat(summary.reserved()).isZero();
        assertThat(summary.brands()).isEmpty();
        assertThat(page.items()).isEmpty();
    }

    @Test
    @WithMockUser(authorities = "vehicles:read")
    void httpContractExposesPageAndOverviewAndRejectsInvalidValues() throws Exception {

        /* Arrange */

        var controller = new VehicleController(
                mock(CreateVehicleUseCase.class),
                mock(GetVehicleUseCase.class),
                service,
                mock(UpdateVehicleUseCase.class));
        var mvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var json = new ObjectMapper();

        /* Act */

        var page = mvc.perform(get("/api/vehicles/page")
                        .param("status", "RESERVED")
                        .param("brand", "HONDA"))
                .andReturn();
        var overview = mvc.perform(get("/api/vehicles/overview"))
                .andReturn();
        var badStatus = mvc.perform(get("/api/vehicles/page")
                        .param("status", "INVALIDO"))
                .andReturn();
        var badSize = mvc.perform(get("/api/vehicles/page")
                        .param("size", "101"))
                .andReturn();

        /* Assert */

        assertThat(page.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readTree(page.getResponse().getContentAsString())
                .path("totalElements").asInt()).isEqualTo(4);
        assertThat(overview.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readTree(overview.getResponse().getContentAsString())
                .path("total").asInt()).isEqualTo(13);
        assertThat(json.readTree(overview.getResponse().getContentAsString())
                .path("available").asInt()).isEqualTo(7);
        assertThat(badStatus.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(badStatus.getResponse().getContentAsString())
                .path("errorCode").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(badSize.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(badSize.getResponse().getContentAsString())
                .path("errorCode").asText()).isEqualTo("VALIDATION_ERROR");
    }
}
