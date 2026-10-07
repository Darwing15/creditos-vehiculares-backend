package pe.sotomayor.creditos.credit.domain.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import pe.sotomayor.creditos.credit.domain.model.entity.CreditApplication;
import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;
import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import pe.sotomayor.creditos.shared.config.JpaAuditConfig;
import pe.sotomayor.creditos.support.PostgresTestContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({PostgresTestContainer.class, JpaAuditConfig.class})
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class CreditApplicationRepositoryTest {

    @Autowired
    private CreditApplicationRepository
            creditApplicationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID pendingApplicationCode;

    private UUID approvedApplicationCode;

    private Long customerId;

    private Long pendingVehicleId;

    private Long approvedVehicleId;

    private Long deletedVehicleId;

    @BeforeEach
    void setUp() {

        customerId =
                insertCustomer();

        pendingVehicleId =
                insertVehicle(
                        UUID.fromString(
                                "550e8400-e29b-41d4-a716-446655440011"
                        ),
                        "CHASSIS-TEST-001",
                        "ENGINE-TEST-001"
                );

        approvedVehicleId =
                insertVehicle(
                        UUID.fromString(
                                "550e8400-e29b-41d4-a716-446655440012"
                        ),
                        "CHASSIS-TEST-002",
                        "ENGINE-TEST-002"
                );

        deletedVehicleId =
                insertVehicle(
                        UUID.fromString(
                                "550e8400-e29b-41d4-a716-446655440013"
                        ),
                        "CHASSIS-TEST-003",
                        "ENGINE-TEST-003"
                );

        pendingApplicationCode = UUID.fromString(
                "550e8400-e29b-41d4-a716-446655440001"
        );

        approvedApplicationCode = UUID.fromString(
                "550e8400-e29b-41d4-a716-446655440002"
        );

        CreditApplication pendingApplication =
                createApplication(
                        pendingApplicationCode,
                        "CA-00000001",
                        pendingVehicleId
                );

        CreditApplication approvedApplication =
                createApplication(
                        approvedApplicationCode,
                        "CA-00000002",
                        approvedVehicleId
                );

        approvedApplication.approve(
                OffsetDateTime.of(
                        2026,
                        8,
                        24,
                        10,
                        0,
                        0,
                        0,
                        ZoneOffset.ofHours(-5)
                ),
                1L
        );

        CreditApplication deletedApplication =
                createApplication(
                        UUID.fromString(
                                "550e8400-e29b-41d4-a716-446655440003"
                        ),
                        "CA-00000003",
                        deletedVehicleId
                );

        deletedApplication.setDeleted(true);

        deletedApplication.setDeletedAt(
                LocalDateTime.of(
                        2026,
                        8,
                        24,
                        10,
                        30
                )
        );

        creditApplicationRepository.save(
                pendingApplication
        );

        creditApplicationRepository.save(
                approvedApplication
        );

        creditApplicationRepository.save(
                deletedApplication
        );

        creditApplicationRepository.flush();
    }

    @Test
    void shouldFindApplicationByApplicationCode() {

        /* Arrange */

        /* Act */

        Optional<CreditApplication> result =
                creditApplicationRepository.findByApplicationCode(
                        pendingApplicationCode
                );

        /* Assert */

        assertThat(result)
                .isPresent();

        assertThat(result.get().getApplicationCode())
                .isEqualTo(pendingApplicationCode);

        assertThat(result.get().getApplicationNumber())
                .isEqualTo("CA-00000001");
    }

    @Test
    void shouldReturnEmptyWhenApplicationCodeDoesNotExist() {

        /* Arrange */

        UUID unknownApplicationCode =
                UUID.randomUUID();

        /* Act */

        Optional<CreditApplication> result =
                creditApplicationRepository.findByApplicationCode(
                        unknownApplicationCode
                );

        /* Assert */

        assertThat(result)
                .isEmpty();
    }

    @Test
    void shouldFindApplicationByApplicationNumber() {

        /* Arrange */

        /* Act */

        Optional<CreditApplication> result =
                creditApplicationRepository.findByApplicationNumber(
                        "CA-00000002"
                );

        /* Assert */

        assertThat(result)
                .isPresent();

        assertThat(result.get().getApplicationNumber())
                .isEqualTo("CA-00000002");

        assertThat(result.get().getStatus())
                .isEqualTo(
                        CreditApplicationStatus.APPROVED
                );
    }

    @Test
    void shouldReturnEmptyWhenApplicationNumberDoesNotExist() {

        /* Arrange */

        /* Act */

        Optional<CreditApplication> result =
                creditApplicationRepository.findByApplicationNumber(
                        "CA-99999999"
                );

        /* Assert */

        assertThat(result)
                .isEmpty();
    }

    @Test
    void shouldReturnTrueWhenApplicationCodeExists() {

        /* Arrange */

        /* Act */

        boolean exists =
                creditApplicationRepository
                        .existsByApplicationCode(
                                pendingApplicationCode
                        );

        /* Assert */

        assertThat(exists)
                .isTrue();
    }

    @Test
    void shouldReturnFalseWhenApplicationCodeDoesNotExist() {

        /* Arrange */

        /* Act */

        boolean exists =
                creditApplicationRepository
                        .existsByApplicationCode(
                                UUID.randomUUID()
                        );

        /* Assert */

        assertThat(exists)
                .isFalse();
    }

    @Test
    void shouldReturnTrueWhenApplicationNumberExists() {

        /* Arrange */

        /* Act */

        boolean exists =
                creditApplicationRepository
                        .existsByApplicationNumber(
                                "CA-00000001"
                        );

        /* Assert */

        assertThat(exists)
                .isTrue();
    }

    @Test
    void shouldReturnFalseWhenApplicationNumberDoesNotExist() {

        /* Arrange */

        /* Act */

        boolean exists =
                creditApplicationRepository
                        .existsByApplicationNumber(
                                "CA-99999999"
                        );

        /* Assert */

        assertThat(exists)
                .isFalse();
    }

    @Test
    void shouldFindApplicationsByStatus() {

        /* Arrange */

        /* Act */

        List<CreditApplication> result =
                creditApplicationRepository
                        .findAllByStatus(
                                CreditApplicationStatus.PENDING
                        );

        /* Assert */

        assertThat(result)
                .hasSize(1);

        assertThat(result.get(0).getApplicationNumber())
                .isEqualTo("CA-00000001");

        assertThat(result.get(0).getStatus())
                .isEqualTo(
                        CreditApplicationStatus.PENDING
                );
    }

    @Test
    void shouldNotReturnDeletedApplication() {

        /* Arrange */

        /* Act */

        Optional<CreditApplication> result =
                creditApplicationRepository
                        .findByApplicationNumber(
                                "CA-00000003"
                        );

        /* Assert */

        assertThat(result)
                .isEmpty();
    }

    @Test
    void shouldNotIncludeDeletedApplicationWhenFindingByStatus() {

        /* Arrange */

        /* Act */

        List<CreditApplication> result =
                creditApplicationRepository
                        .findAllByStatus(
                                CreditApplicationStatus.PENDING
                        );

        /* Assert */

        assertThat(result)
                .hasSize(1);

        assertThat(result)
                .extracting(
                        CreditApplication::getApplicationNumber
                )
                .containsExactly(
                        "CA-00000001"
                );

        assertThat(result)
                .extracting(
                        CreditApplication::getDeleted
                )
                .containsOnly(false);
    }

    @Test
    void shouldScopeReadAndLockedResolutionByBranch() {

        /* Arrange */

        /* Act */

        /* Assert */

        assertThat(creditApplicationRepository
                .findByApplicationCodeAndBranchId(
                        pendingApplicationCode,
                        1L))
                .isPresent();

        assertThat(creditApplicationRepository
                .findByApplicationCodeAndBranchId(
                        pendingApplicationCode,
                        2L))
                .isEmpty();

        assertThat(creditApplicationRepository
                .findByApplicationCodeAndBranchIdForUpdate(
                        pendingApplicationCode,
                        1L))
                .isPresent();

        assertThat(creditApplicationRepository
                .findByApplicationCodeAndBranchIdForUpdate(
                        pendingApplicationCode,
                        2L))
                .isEmpty();
    }

    @Test
    void shouldExcludeOtherBranchesFromStatusList() {

        /* Arrange */

        /* Act */

        /* Assert */

        assertThat(creditApplicationRepository
                .findAllByStatusAndBranchId(
                        CreditApplicationStatus.PENDING,
                        1L))
                .allMatch(application -> application.getBranchId()
                        .equals(1L)).hasSize(1);

        assertThat(creditApplicationRepository
                .findAllByStatusAndBranchId(
                        CreditApplicationStatus.PENDING,
                        2L))
                .isEmpty();
    }

    private CreditApplication createApplication(
            UUID applicationCode,
            String applicationNumber,
            Long vehicleId) {

        return CreditApplication.create(
                applicationCode,
                applicationNumber,
                FinancingCurrency.PEN,
                new BigDecimal("12000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("1.50"),
                12,
                customerId,
                vehicleId,
                1L
        );
    }

    private Long insertCustomer() {

        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO customers (
                            customer_code,
                            full_name,
                            document_type,
                            document_number,
                            branch_id
                        )
                        VALUES (?, ?, ?, ?, ?)
                        RETURNING id
                        """,
                Long.class,
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440010"
                ),
                "Cliente Repository Test",
                "DNI",
                "70000001",
                1L
        );
    }

    private Long insertVehicle(
            UUID vehicleCode,
            String chassisNumber,
            String engineNumber) {

        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO vehicles (
                            vehicle_code,
                            vehicle_type,
                            brand,
                            model,
                            manufacturing_year,
                            color,
                            chassis_number,
                            engine_number,
                            reference_price,
                            branch_id,
                            status
                        )
                        VALUES (
                            ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'RESERVED'
                        )
                        RETURNING id
                        """,
                Long.class,
                vehicleCode,
                "MOTORCYCLE",
                "Honda",
                "Wave 110S",
                2026,
                "Black",
                chassisNumber,
                engineNumber,
                new BigDecimal("12000.00"),
                1L
        );
    }
}
