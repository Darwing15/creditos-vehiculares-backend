package pe.sotomayor.creditos.security.infrastructure.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.sotomayor.creditos.credit.presentation.controller.CreditApplicationController;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.ApproveCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.CancelCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.CreateCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.GetCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.ListCreditApplicationsUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.RejectCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditsimulation.SimulateCreditApplicationUseCase;

import pe.sotomayor.creditos.customers.application.mapper.CustomerMapper;
import pe.sotomayor.creditos.customers.application.usecase.CreateCustomerUseCase;
import pe.sotomayor.creditos.customers.application.usecase.GetCustomerUseCase;
import pe.sotomayor.creditos.customers.application.usecase.ListCustomersUseCase;
import pe.sotomayor.creditos.customers.application.usecase.UpdateCustomerUseCase;
import pe.sotomayor.creditos.customers.domain.model.entity.Customer;
import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;
import pe.sotomayor.creditos.customers.presentation.CustomerController;

import pe.sotomayor.creditos.vehicles.application.mapper.VehicleMapper;
import pe.sotomayor.creditos.vehicles.application.usecase.CreateVehicleUseCase;
import pe.sotomayor.creditos.vehicles.application.usecase.GetVehicleUseCase;
import pe.sotomayor.creditos.vehicles.application.usecase.ListVehiclesUseCase;
import pe.sotomayor.creditos.vehicles.application.usecase.UpdateVehicleUseCase;
import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;
import pe.sotomayor.creditos.vehicles.presentation.VehicleController;

import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@WebMvcTest(controllers = {
        CreditApplicationController.class,
        CustomerController.class,
        VehicleController.class},
        properties = {
                "app.security.allowed-origins=http://localhost:5173,https://ksj9bd7g-5173.brs.devtunnels.ms"
        })
@Import({SecurityConfig.class, SecurityErrorHandler.class})
class CreditCatalogAuthorizationTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtService jwt;

    @MockitoBean
    CustomUserDetailsService users;

    @MockitoBean
    CreateCreditApplicationUseCase create;

    @MockitoBean
    GetCreditApplicationUseCase get;

    @MockitoBean
    ListCreditApplicationsUseCase list;

    @MockitoBean
    ApproveCreditApplicationUseCase approve;

    @MockitoBean
    RejectCreditApplicationUseCase reject;

    @MockitoBean
    CancelCreditApplicationUseCase cancel;

    @MockitoBean
    SimulateCreditApplicationUseCase simulate;

    @MockitoBean
    ListCustomersUseCase customers;

    @MockitoBean
    ListVehiclesUseCase vehicles;

    @MockitoBean
    CreateCustomerUseCase createCustomer;

    @MockitoBean
    UpdateCustomerUseCase updateCustomer;

    @MockitoBean
    GetCustomerUseCase getCustomer;

    @MockitoBean
    CreateVehicleUseCase createVehicle;

    @MockitoBean
    UpdateVehicleUseCase updateVehicle;

    @MockitoBean
    GetVehicleUseCase getVehicle;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/customers",
            "/api/vehicles",
            "/api/credit-applications?status=PENDING"
    })
    void anonymousCannotRead(String path) throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(get(path))
                .andExpect(status().isUnauthorized());

        /* Assert */

        verifyNoInteractions(customers, vehicles, list);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/customers",
            "/api/vehicles",
            "/api/credit-applications?status=PENDING"
    })
    @WithMockUser(roles = "ADMIN")
    void roleNameDoesNotGrantAccess(String path) throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(get(path))
                .andExpect(status().isForbidden());

        /* Assert */

        verifyNoInteractions(customers, vehicles, list);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/customers",
            "/api/vehicles",
            "/api/credit-applications?status=PENDING"
    })
    @WithMockUser(authorities = {
            "customers:read",
            "vehicles:read",
            "credit-applications:read"
    })
    void authoritiesEnableRead(String path) throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(get(path))
                .andExpect(status().isOk());

        /* Assert */
    }

    @Test
    @WithMockUser(authorities = "credit-applications:read")
    void readDoesNotGrantCreateOrResolve() throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(post("/api/credit-applications"))
                .andExpect(status().isForbidden());

        mvc.perform(patch(
                        "/api/credit-applications/550e8400-e29b-41d4-a716-446655440001/approve"))
                .andExpect(status().isForbidden());

        /* Assert */

        verifyNoInteractions(create, approve);
    }

    @Test
    @WithMockUser(authorities = "credit-applications:create")
    void createReachesValidation() throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(post("/api/credit-applications")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/credit-applications/simulations")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());

        /* Assert */

        verifyNoInteractions(create, simulate);
    }

    @Test
    @WithMockUser(authorities = "credit-applications:resolve")
    void resolveReachesController() throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(patch(
                        "/api/credit-applications/550e8400-e29b-41d4-a716-446655440001/approve"))
                .andExpect(status().isOk());

        /* Assert */
    }

    @Test
    @WithMockUser(authorities = {
            "customers:read",
            "vehicles:read",
            "credit-applications:read"
    })
    void unregisteredRoutesRemainDenied() throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(get("/api/unregistered"))
                .andExpect(status().isForbidden());

        /* Assert */
    }

    @Test
    void tunnelPreflightIsAllowedWithoutAuthentication() throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(options("/api/credit-applications")
                        .header("Origin", "https://ksj9bd7g-5173.brs.devtunnels.ms")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "https://ksj9bd7g-5173.brs.devtunnels.ms"));

        /* Assert */
    }

    @ParameterizedTest
    @ValueSource(strings = {"customers", "vehicles"})
    void anonymousCannotWriteCatalogs(String module) throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(post("/api/" + module))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/"
                        + module + "/550e8400-e29b-41d4-a716-446655440001"))
                .andExpect(status().isUnauthorized());

        /* Assert */

        verifyNoInteractions(
                createCustomer,
                updateCustomer,
                createVehicle,
                updateVehicle
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"customers", "vehicles"})
    @WithMockUser(roles = "ADMIN")
    void adminNameAloneDoesNotGrantCatalogWrites(String module) throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(post("/api/" + module))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/"
                        + module + "/550e8400-e29b-41d4-a716-446655440001"))
                .andExpect(status().isForbidden());

        /* Assert */

        verifyNoInteractions(
                createCustomer,
                updateCustomer,
                createVehicle,
                updateVehicle
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"customers", "vehicles"})
    @WithMockUser(authorities = {"customers:read", "vehicles:read"})
    void readAloneCannotWriteCatalogs(String module) throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(post("/api/" + module))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/"
                        + module + "/550e8400-e29b-41d4-a716-446655440001"))
                .andExpect(status().isForbidden());

        /* Assert */

        verifyNoInteractions(
                createCustomer,
                updateCustomer,
                createVehicle,
                updateVehicle
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"customers", "vehicles"})
    @WithMockUser(authorities = {"customers:write", "vehicles:write"})
    void writeAllowsValidationButDoesNotGrantRead(String module) throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(post("/api/" + module)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/"
                + module + "/550e8400-e29b-41d4-a716-446655440001")
                .contentType("application/json")
                .content("{}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/"
                        + module + "/550e8400-e29b-41d4-a716-446655440001"))
                .andExpect(status().isForbidden());

        /* Assert */

        verifyNoInteractions(
                createCustomer,
                updateCustomer,
                createVehicle,
                updateVehicle
        );
    }

    @Test
    @WithMockUser(authorities = "customers:write")
    void customerWriterCannotWriteVehicles() throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(post("/api/vehicles"))
                .andExpect(status().isForbidden());

        /* Assert */

        verifyNoInteractions(createVehicle);
    }

    @Test
    @WithMockUser(authorities = "vehicles:write")
    void vehicleWriterCannotWriteCustomers() throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(post("/api/customers"))
                .andExpect(status().isForbidden());

        /* Assert */

        verifyNoInteractions(createCustomer);
    }

    @ParameterizedTest
    @ValueSource(strings = {"customers", "vehicles"})
    @WithMockUser(authorities = {"customers:read", "vehicles:read"})
    void detailUsesTheModuleReadAuthority(String module) throws Exception {

        /* Arrange */

        /* Act */

        mvc.perform(get("/api/"
                        + module + "/550e8400-e29b-41d4-a716-446655440001"))
                .andExpect(status().isOk());

        /* Assert */
    }

    @Test
    @WithMockUser(authorities = "customers:read")
    void shouldIncludePublicCodeAndAddressWhenListingCustomers() throws Exception {

        /* Arrange */

        var customer = createCustomer();
        var mapper = Mappers.getMapper(CustomerMapper.class);
        when(customers.listActive())
                .thenReturn(List.of(mapper.toOption(customer)));

        /* Act */

        var result = mvc.perform(get("/api/customers"))
                .andReturn();

        /* Assert */

        var record = new ObjectMapper().readTree(
                result.getResponse().getContentAsString()).get(0);
        assertThat(result.getResponse().getStatus())
                .isEqualTo(200);
        assertThat(record.get("id").asLong())
                .isEqualTo(21L);
        assertThat(record.get("customerCode").asText())
                .isEqualTo(customer.getCustomerCode().toString());
        assertThat(record.get("address").asText())
                .isEqualTo("Jr. Progreso 123");
        assertThat(record.get("documentNumber").asText())
                .isEqualTo("01234567");

        verify(customers).listActive();

        verifyNoMoreInteractions(customers);
    }

    @Test
    @WithMockUser(authorities = "vehicles:read")
    void shouldIncludePhysicalIdentifiersWhenListingVehicles() throws Exception {

        /* Arrange */

        var vehicle = createVehicle();
        var mapper = Mappers.getMapper(VehicleMapper.class);
        when(vehicles.list(null))
                .thenReturn(List.of(mapper.toOption(vehicle)));

        /* Act */

        var result = mvc.perform(get("/api/vehicles"))
                .andReturn();

        /* Assert */

        var record = new ObjectMapper().readTree(
                result.getResponse().getContentAsString()).get(0);
        assertThat(result.getResponse().getStatus())
                .isEqualTo(200);
        assertThat(record.get("vehicleCode").asText())
                .isEqualTo(vehicle.getVehicleCode().toString());
        assertThat(record.get("chassisNumber").asText())
                .isEqualTo("VIN-001");
        assertThat(record.get("engineNumber").asText())
                .isEqualTo("ENGINE-001");
        assertThat(record.get("referencePrice").decimalValue())
                .isEqualByComparingTo("12000.50");

        verify(vehicles).list(null);

        verifyNoMoreInteractions(vehicles);
    }

    private Customer createCustomer() {
        var customer = Customer.create(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440071"),
                "Cliente web",
                CustomerDocumentType.DNI,
                "01234567",
                null,
                null,
                null,
                null,
                "Jr. Progreso 123",
                null,
                3L
        );
        customer.setId(21L);
        return customer;
    }

    private Vehicle createVehicle() {
        var vehicle = Vehicle.create(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440072"),
                "MOTO",
                "Bajaj",
                "NS200",
                2026,
                "Negro",
                "VIN-001",
                "ENGINE-001",
                null,
                new BigDecimal("12000.50"),
                3L
        );
        vehicle.setId(22L);
        return vehicle;
    }
}

