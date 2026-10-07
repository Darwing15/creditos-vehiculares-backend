package pe.sotomayor.creditos.catalog.integration;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.support.PostgresTestContainer;
import pe.sotomayor.creditos.shared.config.JpaAuditConfig;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.error.ErrorCode;

import pe.sotomayor.creditos.customers.application.dto.request.CreateCustomerRequest;
import pe.sotomayor.creditos.customers.application.dto.request.UpdateCustomerRequest;
import pe.sotomayor.creditos.customers.application.service.CreateCustomerService;
import pe.sotomayor.creditos.customers.application.service.GetCustomerService;
import pe.sotomayor.creditos.customers.application.service.UpdateCustomerService;
import pe.sotomayor.creditos.customers.application.mapper.CustomerMapperImpl;

import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;
import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;

import pe.sotomayor.creditos.customers.infrastructure.config.CustomerDomainConfig;

import pe.sotomayor.creditos.vehicles.application.dto.request.CreateVehicleRequest;
import pe.sotomayor.creditos.vehicles.application.dto.request.UpdateVehicleRequest;
import pe.sotomayor.creditos.vehicles.application.service.CreateVehicleService;
import pe.sotomayor.creditos.vehicles.application.service.GetVehicleService;
import pe.sotomayor.creditos.vehicles.application.service.UpdateVehicleService;
import pe.sotomayor.creditos.vehicles.application.mapper.VehicleMapperImpl;

import pe.sotomayor.creditos.vehicles.domain.repository.VehicleRepository;
import pe.sotomayor.creditos.vehicles.infrastructure.config.VehicleDomainConfig;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestContainer.class, JpaAuditConfig.class,
        CreateCustomerService.class, UpdateCustomerService.class,
        GetCustomerService.class, CustomerMapperImpl.class,
        CustomerDomainConfig.class, CreateVehicleService.class,
        UpdateVehicleService.class, GetVehicleService.class,
        VehicleMapperImpl.class, VehicleDomainConfig.class,
        CatalogPersistenceTest.MethodSecurity.class})
@WithMockUser(authorities = {
        "customers:read",
        "customers:write",
        "vehicles:read",
        "vehicles:write"
})
class CatalogPersistenceTest {
    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurity {
    }

    @Autowired
    CreateCustomerService createCustomer;

    @Autowired
    UpdateCustomerService updateCustomer;

    @Autowired
    GetCustomerService getCustomer;

    @Autowired
    CustomerRepository customers;

    @Autowired
    CreateVehicleService createVehicle;

    @Autowired
    UpdateVehicleService updateVehicle;

    @Autowired
    GetVehicleService getVehicle;

    @Autowired
    VehicleRepository vehicles;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager entityManager;

    @MockitoBean
    CurrentBranchProvider branches;

    Long branch;

    @BeforeEach
    void setUp() {
        branch = jdbc.queryForObject("insert into branches (code,name,address,phone,is_active) " +
                "values ('CATALOG-TEST','Catalog Test','Direccion','900000000',true) returning id", Long.class);

        when(branches.getCurrentBranchId()).thenReturn(branch);
    }

    CreateCustomerRequest customerRequest() {
        return new CreateCustomerRequest(
                "Cliente",
                CustomerDocumentType.DNI,
                "CT-12345678",
                null,
                null,
                "Comerciante",
                new BigDecimal("2000.00"),
                null,
                null
        );
    }

    UpdateCustomerRequest customerUpdate(String revision) {
        return new UpdateCustomerRequest(
                "Cliente editado",
                "900000001",
                null,
                null,
                new BigDecimal("2500.00"),
                "Nueva direccion",
                null,
                revision
        );
    }

    CreateVehicleRequest vehicleRequest(String chassis,
                                        String engine) {

        return new CreateVehicleRequest(
                "MOTO",
                "Bajaj",
                "NS200",
                2026,
                "Negro",
                chassis,
                engine,
                null,
                new BigDecimal("12000.00")
        );
    }

    UpdateVehicleRequest vehicleUpdate(String revision) {
        return new UpdateVehicleRequest(
                "MOTO",
                "Bajaj",
                "NS200",
                2026,
                "Rojo",
                "CT-CHASSIS-1",
                "CT-ENGINE-1",
                null,
                new BigDecimal("13000.00"),
                revision
        );
    }

    @Test
    void customerCreateAndUpdatePersistAndPreserveItsDocument() {
        var created = createCustomer.create(customerRequest());
        var updated = updateCustomer.update(created.customerCode(),
                customerUpdate(created.revision()));
        entityManager.flush();
        entityManager.clear();
        var detail = getCustomer.get(created.customerCode());

        assertThat(detail.fullName())
                .isEqualTo("Cliente editado");
        assertThat(detail.documentNumber())
                .isEqualTo("CT-12345678");
        assertThat(detail.branchId())
                .isEqualTo(branch);
        assertThat(detail.revision())
                .isEqualTo(updated.revision())
                .isNotEqualTo(created.revision());
    }

    @Test
    void vehicleCreateAndUpdatePersistAndPreserveAvailableState() {
        var created = createVehicle
                .create(
                        vehicleRequest("CT-CHASSIS-1",
                                "CT-ENGINE-1"));
        var updated = updateVehicle
                .update(created.vehicleCode(),
                        vehicleUpdate(created.revision()));
        entityManager.flush();
        entityManager.clear();
        var detail = getVehicle
                .get(created.vehicleCode());

        assertThat(detail.referencePrice()).isEqualByComparingTo("13000.00");
        assertThat(detail.status().name()).isEqualTo("AVAILABLE");
        assertThat(detail.revision()).isEqualTo(updated.revision());
    }

    @Test
    void staleCustomerAndVehicleFormsCannotOverwriteAnEarlierSave() {

        /* Arrange */

        var customer = createCustomer
                .create(customerRequest());
        updateCustomer.update(customer.customerCode(),
                customerUpdate(customer.revision()));

        var vehicle = createVehicle.create(
                vehicleRequest("CT-CHASSIS-1",
                        "CT-ENGINE-1"));
        updateVehicle.update(vehicle.vehicleCode(),
                vehicleUpdate(vehicle.revision()));

        /* Act */

        Throwable throwablec = catchThrowable(
                () -> updateCustomer.update(customer.customerCode(),
                        customerUpdate(customer.revision()))
        );

        Throwable throwablev = catchThrowable(
                () -> updateVehicle.update(vehicle.vehicleCode(),
                        vehicleUpdate(vehicle.revision()))
        );

        /* Assert */

        assertThat(throwablec)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);


        assertThat(throwablev)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);
    }

    @Test
    void scopedQueriesAndServicesHideOtherBranches() {

        /* Arrange */

        var customer = createCustomer.create(customerRequest());
        var vehicle = createVehicle.create(vehicleRequest("CT-CHASSIS-1", "CT-ENGINE-1"));
        when(branches.getCurrentBranchId()).thenReturn(branch + 100L);

        /* Act */

        Throwable throwablegc = catchThrowable(
                () -> getCustomer.get(customer.customerCode())
        );

        Throwable throwableuc = catchThrowable(
                () -> updateCustomer.update(
                        customer.customerCode(),
                        customerUpdate(customer.revision()))
        );

        Throwable throwablegv = catchThrowable(
                () -> getVehicle.get(vehicle.vehicleCode())
        );

        Throwable throwableuv = catchThrowable(
                () -> updateVehicle.update(
                        vehicle.vehicleCode(),
                        vehicleUpdate(vehicle.revision()))
        );

        /* Assert */

        assertThat(throwablegc)
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(throwableuc)
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(throwablegv)
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(throwableuv)
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void softDeletedIdentitiesRemainUniqueAndAreHiddenByScopedQueries() {

        /* Arrange */

        var customer = createCustomer.create(customerRequest());
        var vehicle = createVehicle.create(
                vehicleRequest("CT-CHASSIS-1",
                        "CT-ENGINE-1"));
        jdbc.update("update customers set is_deleted=true,deleted_at=now() where id=?",
                customer.id());
        jdbc.update("update vehicles set is_deleted=true,deleted_at=now() where id=?",
                vehicle.id());
        entityManager.clear();

        /* Act */

        Throwable throwablecc = catchThrowable(
                () -> createCustomer.create(customerRequest())
        );

        Throwable throwablecv = catchThrowable(
                () -> createVehicle.create(
                        vehicleRequest("CT-CHASSIS-1",
                                "new-engine"))
        );

        /* Assert */

        assertThat(customers.findByCustomerCodeAndBranchId(
                customer.customerCode(), branch))
                .isEmpty();
        assertThat(vehicles.findByVehicleCodeAndBranchId(
                vehicle.vehicleCode(), branch))
                .isEmpty();
        assertThat(customers.existsDocumentIncludingDeleted(
                "DNI", "CT-12345678"))
                .isTrue();
        assertThat(vehicles.existsIdentifiersIncludingDeleted(
                "CT-CHASSIS-1", "other", 0L))
                .isTrue();
        assertThat(vehicles.existsIdentifiersIncludingDeleted(
                "other", "CT-ENGINE-1", 0L))
                .isTrue();
        assertThat(throwablecc)
                .isInstanceOf(ConflictException.class);
        assertThat(throwablecv)
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void vehicleDuplicateCheckExcludesOnlyTheEditedUnit() {
        var first = createVehicle.create(vehicleRequest("CT-CHASSIS-1", "CT-ENGINE-1"));
        createVehicle.create(vehicleRequest("CT-CHASSIS-2", "CT-ENGINE-2"));
        assertThat(vehicles.existsIdentifiersIncludingDeleted("CT-CHASSIS-1", "CT-ENGINE-1", first.id())).isFalse();
        assertThat(vehicles.existsIdentifiersIncludingDeleted("CT-CHASSIS-2", "CT-ENGINE-1", first.id())).isTrue();
        assertThat(vehicles.existsIdentifiersIncludingDeleted("CT-CHASSIS-1", "CT-ENGINE-2", first.id())).isTrue();
    }

    @Test
    void reservationChangesRevisionAndBlocksGenericVehicleEdit() {

        /* Arrange */

        var created = createVehicle.create(
                vehicleRequest("CT-CHASSIS-1",
                        "CT-ENGINE-1"));
        vehicles.findByIdAndBranchIdForUpdate(
                        created.id(), branch)
                .orElseThrow().reserveForFinancing();
        entityManager.flush();
        entityManager.clear();
        var detail = getVehicle.get(created.vehicleCode());

        /* Act */

        Throwable throwable = catchThrowable(
                () -> updateVehicle.update(
                        created.vehicleCode(),
                        vehicleUpdate(detail.revision()))
        );

        /* Assert */

        assertThat(detail.revision())
                .isNotEqualTo(created.revision());

        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_NOT_EDITABLE);
    }

    @Test
    @WithMockUser(authorities = {"customers:read", "vehicles:read"})
    void methodSecurityRejectsDirectWritesEvenWithoutAController() {
        /* Arrange */

        /* Act */

        Throwable throwablecc = catchThrowable(
                () -> createCustomer.create(customerRequest())
        );

        Throwable throwablecv = catchThrowable(
                () -> createVehicle.create(
                        vehicleRequest("CT-CHASSIS-1",
                                "CT-ENGINE-1"))
        );

        Throwable throwableuc = catchThrowable(
                () -> updateCustomer.update(
                        UUID.randomUUID(),
                        customerUpdate("0".repeat(64)))
        );

        Throwable throwableuv = catchThrowable(
                () -> updateVehicle.update(
                        UUID.randomUUID(),
                        vehicleUpdate("0".repeat(64)))
        );

        /* Assert */

        assertThat(throwablecc)
                .isInstanceOf(AccessDeniedException.class);

        assertThat(throwablecv)
                .isInstanceOf(AccessDeniedException.class);

        assertThat(throwableuc)
                .isInstanceOf(AccessDeniedException.class);

        assertThat(throwableuv)
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void soldVehicleCannotBeEdited() {

        /* Arrange */

        var created = createVehicle.create(
                vehicleRequest("CT-CHASSIS-1",
                        "CT-ENGINE-1"));
        jdbc.update("update vehicles set status='SOLD'::vehicle_status where id=?",
                created.id());
        entityManager.clear();
        var detail = getVehicle.get(created.vehicleCode());

        /* Act */

        Throwable throwable = catchThrowable(
                () -> updateVehicle.update(created.vehicleCode(),
                        vehicleUpdate(detail.revision()))
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_NOT_EDITABLE);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousVehicleUpdatesAllowOnlyOneSaveFromTheSameRevision() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        try {
            var created = createVehicle.create(vehicleRequest("CT-CHASSIS-1", "CT-ENGINE-1"));
            var start = new CountDownLatch(1);
            Callable<String> attempt = () -> {
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(
                        "test-writer", null,
                        List.of(new SimpleGrantedAuthority("vehicles:write"))));
                SecurityContextHolder.setContext(context);
                try {
                    if (!start.await(10, TimeUnit.SECONDS))
                        throw new IllegalStateException("Start timed out");
                    updateVehicle.update(created.vehicleCode(),
                            vehicleUpdate(created.revision()));
                    return "saved";
                } catch (ConflictException failure) {
                    return failure.getErrorCode().name();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            };
            var first = pool.submit(attempt);
            var second = pool.submit(attempt);
            start.countDown();

            assertThat(List.of(first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("saved", "CONCURRENT_MODIFICATION");

            assertThat(getVehicle.get(created.vehicleCode()).referencePrice())
                    .isEqualByComparingTo("13000.00");
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
            jdbc.update("delete from vehicles where branch_id=?", branch);
            jdbc.update("delete from branches where id=?", branch);
        }
    }
}

