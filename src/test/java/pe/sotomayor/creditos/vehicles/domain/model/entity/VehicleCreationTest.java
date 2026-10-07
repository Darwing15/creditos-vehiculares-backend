package pe.sotomayor.creditos.vehicles.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;
import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleCreationTest {

    @Test
    void shouldCreateAvailableVehicleWithRegistrationDataAndNoPersistenceIdentity() {

        /* Arrange */

        UUID code = createCode();

        /* Act */

        Vehicle vehicle = Vehicle.create(
                code,
                "MOTO",
                "Bajaj",
                "Pulsar NS200",
                2026,
                "Negro",
                "CHASSIS-001",
                "ENGINE-001",
                "DUA-001",
                new BigDecimal("12500.00"),
                3L
        );

        /* Assert */

        assertThat(vehicle.getVehicleCode())
                .isEqualTo(code);
        assertThat(vehicle.getVehicleType())
                .isEqualTo("MOTO");
        assertThat(vehicle.getBrand())
                .isEqualTo("Bajaj");
        assertThat(vehicle.getModel())
                .isEqualTo("Pulsar NS200");
        assertThat(vehicle.getManufacturingYear())
                .isEqualTo(2026);
        assertThat(vehicle.getColor())
                .isEqualTo("Negro");
        assertThat(vehicle.getChassisNumber())
                .isEqualTo("CHASSIS-001");
        assertThat(vehicle.getEngineNumber())
                .isEqualTo("ENGINE-001");
        assertThat(vehicle.getDuaNumber())
                .isEqualTo("DUA-001");
        assertThat(vehicle.getReferencePrice())
                .isEqualByComparingTo("12500.00");
        assertThat(vehicle.getBranchId())
                .isEqualTo(3L);
        assertThat(vehicle.getStatus())
                .isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(vehicle.getDeleted())
                .isFalse();
        assertThat(vehicle.getId())
                .isNull();
        assertThat(vehicle.getCreatedAt())
                .isNull();
    }

    @Test
    void shouldAllowAbsentDuaAndReserveThroughDomainOperation() {

        /* Arrange */

        Vehicle vehicle = createVehicle(
                createCode(),
                "MOTO",
                "Bajaj",
                "Pulsar NS200",
                2026,
                "Negro",
                "CHASSIS-001",
                "ENGINE-001",
                null,
                new BigDecimal("12500.00"),
                3L
        );

        /* Act */

        vehicle.reserveForFinancing();

        /* Assert */

        assertThat(vehicle.getDuaNumber())
                .isNull();
        assertThat(vehicle.getStatus())
                .isEqualTo(VehicleStatus.RESERVED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectMissingVehicleType(String type) {

        /* Arrange */

        UUID code = createCode();

        /* Act / Assert */

        assertThatThrownBy(() -> createVehicle(
                code,
                type,
                "Bajaj",
                "Pulsar NS200",
                2026,
                "Negro",
                "CHASSIS-001",
                "ENGINE-001",
                null,
                new BigDecimal("12500.00"),
                3L)
        ).isInstanceOf(ValidationException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-0.01", "10000000000.00", "10.001"})
    void shouldRejectPriceOutsideSchemaConstraints(String amount) {

        /* Arrange */

        BigDecimal price = new BigDecimal(amount);

        /* Act / Assert */

        assertThatThrownBy(() -> createVehicle(
                createCode(),
                "MOTO",
                "Bajaj",
                "Pulsar NS200",
                2026,
                "Negro",
                "CHASSIS-001",
                "ENGINE-001",
                null,
                price,
                3L)
        ).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "code",
            "year",
            "price",
            "branch",
            "brand",
            "model",
            "color",
            "chassis",
            "engine",
            "dua"
    })
    void shouldRejectInvalidRegistrationField(String field) {

        /* Arrange */

        UUID code = field.equals("code") ? null : createCode();
        Integer year = field.equals("year") ? null : 2026;
        BigDecimal price = field.equals("price") ? null : new BigDecimal("12500.00");
        Long branch = field.equals("branch") ? null : 3L;
        String brand = field.equals("brand") ? " " : "Bajaj";
        String model = field.equals("model") ? " " : "Pulsar NS200";
        String color = field.equals("color") ? " " : "Negro";
        String chassis = field.equals("chassis") ? " " : "CHASSIS-001";
        String engine = field.equals("engine") ? " " : "ENGINE-001";
        String dua = field.equals("dua") ? " " : null;

        /* Act / Assert */

        assertThatThrownBy(() -> createVehicle(
                code,
                "MOTO",
                brand,
                model,
                year,
                color,
                chassis,
                engine,
                dua,
                price,
                branch)
        ).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "type",
            "brand",
            "model",
            "color",
            "chassis",
            "engine",
            "dua"
    })
    void shouldRejectTextThatExceedsSchemaLength(String field) {

        /* Arrange */

        String type = field.equals("type") ? "a".repeat(51) : "MOTO";
        String brand = field.equals("brand") ? "a".repeat(101) : "Bajaj";
        String model = field.equals("model") ? "a".repeat(151) : "Pulsar NS200";
        String color = field.equals("color") ? "a".repeat(51) : "Negro";
        String chassis = field.equals("chassis") ? "a".repeat(101) : "CHASSIS-001";
        String engine = field.equals("engine") ? "a".repeat(101) : "ENGINE-001";
        String dua = field.equals("dua") ? "a".repeat(51) : null;

        /* Act / Assert */

        assertThatThrownBy(() -> createVehicle(
                createCode(),
                type,
                brand,
                model,
                2026,
                color,
                chassis,
                engine,
                dua,
                new BigDecimal("12500.00"),
                3L)
        ).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void shouldRejectNonPositiveBranchId(long branchId) {

        /* Arrange */

        UUID code = createCode();

        /* Act / Assert */

        assertThatThrownBy(() -> createVehicle(
                code,
                "MOTO",
                "Bajaj",
                "Pulsar NS200",
                2026,
                "Negro",
                "CHASSIS-001",
                "ENGINE-001",
                null,
                new BigDecimal("12500.00"),
                branchId)
        ).isInstanceOf(ValidationException.class);
    }

    private UUID createCode() {
        return UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
    }

    private Vehicle createVehicle(
            UUID code,
            String type,
            String brand,
            String model,
            Integer year,
            String color,
            String chassis,
            String engine,
            String dua,
            BigDecimal price,
            Long branchId) {

        return Vehicle.create(
                code,
                type,
                brand,
                model,
                year,
                color,
                chassis,
                engine,
                dua,
                price,
                branchId
        );
    }
}

