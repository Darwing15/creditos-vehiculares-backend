package pe.sotomayor.creditos.vehicles.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;

import org.hibernate.type.SqlTypes;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ValidationException;
import pe.sotomayor.creditos.shared.persistence.SoftDeleteEntity;

import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Agregado de una unidad fisica de inventario conforme al esquema V4.
 *
 * @author DarwinG
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "vehicles")
@SQLRestriction("is_deleted = false")
public class Vehicle extends SoftDeleteEntity {

    @Column(
            name = "vehicle_code",
            nullable = false,
            unique = true
    )
    private UUID vehicleCode;

    @Column(
            name = "vehicle_type",
            nullable = false,
            length = 50
    )
    private String vehicleType;

    @Column(
            nullable = false,
            length = 100
    )
    private String brand;

    @Column(
            nullable = false,
            length = 150
    )
    private String model;

    @Column(
            name = "manufacturing_year",
            nullable = false
    )
    private Integer manufacturingYear;

    @Column(
            nullable = false,
            length = 50
    )
    private String color;

    @Column(
            name = "chassis_number",
            nullable = false,
            unique = true,
            length = 100
    )
    private String chassisNumber;

    @Column(
            name = "engine_number",
            nullable = false,
            unique = true,
            length = 100
    )
    private String engineNumber;

    @Column(
            name = "dua_number",
            length = 50
    )
    private String duaNumber;

    @Column(
            name = "reference_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal referencePrice;

    @Column(
            name = "branch_id",
            nullable = false
    )
    private Long branchId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
            nullable = false,
            columnDefinition = "vehicle_status"
    )
    private VehicleStatus status;

    /**
     * Crea una unidad disponible sin aceptar su estado desde el formulario.
     * El UUID y la sucursal proceden del sistema; el id y la auditoria se asignan al persistir.
     * Unicidad de chasis/motor y sucursal existente corresponden al futuro caso de uso.
     *
     * @param vehicleCode       UUID generado por el sistema
     * @param vehicleType       tipo comercial
     * @param brand             marca
     * @param model             modelo
     * @param manufacturingYear anio de fabricacion, requerido sin inferir un rango empresarial
     * @param color             color
     * @param chassisNumber     numero de chasis
     * @param engineNumber      numero de motor
     * @param duaNumber         DUA opcional
     * @param referencePrice    precio positivo que cabe en NUMERIC(12,2)
     * @param branchId          sucursal del contexto autenticado
     * @return unidad AVAILABLE y no eliminada
     */
    public static Vehicle create(
            UUID vehicleCode,
            String vehicleType,
            String brand,
            String model,
            Integer manufacturingYear,
            String color,
            String chassisNumber,
            String engineNumber,
            String duaNumber,
            BigDecimal referencePrice,
            Long branchId) {

        validate(vehicleCode != null, "Vehicle code is required");
        validate(manufacturingYear != null, "Manufacturing year is required");
        validate(branchId != null && branchId > 0, "Branch id must be positive");
        validateRequiredText(vehicleType, 50, "Vehicle type");
        validateRequiredText(brand, 100, "Brand");
        validateRequiredText(model, 150, "Model");
        validateRequiredText(color, 50, "Color");
        validateRequiredText(chassisNumber, 100, "Chassis number");
        validateRequiredText(engineNumber, 100, "Engine number");
        if (duaNumber != null) {
            validateRequiredText(duaNumber, 50, "DUA number");
        }
        validate(referencePrice != null && referencePrice.signum() > 0,
                "Reference price must be positive");
        validate(referencePrice.scale() <= 2 && referencePrice.precision() - referencePrice.scale() <= 10,
                "Reference price must fit NUMERIC(12,2)");

        Vehicle vehicle = new Vehicle();
        vehicle.vehicleCode = vehicleCode;
        vehicle.vehicleType = vehicleType;
        vehicle.brand = brand;
        vehicle.model = model;
        vehicle.manufacturingYear = manufacturingYear;
        vehicle.color = color;
        vehicle.chassisNumber = chassisNumber;
        vehicle.engineNumber = engineNumber;
        vehicle.duaNumber = duaNumber;
        vehicle.referencePrice = referencePrice;
        vehicle.branchId = branchId;
        vehicle.status = VehicleStatus.AVAILABLE;
        return vehicle;
    }

    /**
     * Valida los textos requeridos sin alterar identificadores ni contenido.
     */
    private static void validateRequiredText(
            String value,
            int maxLength,
            String field) {
        validate(
                value != null
                        && !value.isBlank()
                        && value.length() <= maxLength,
                field + " is required and must not exceed " + maxLength + " characters");
    }

    /**
     * Rechaza datos que no cumplen las restricciones de registro.
     */
    private static void validate(
            boolean valid,
            String message) {
        if (!valid) {
            throw new ValidationException(
                    ErrorCode.VALIDATION_ERROR,
                    message
            );
        }
    }

    /**
     * Reserva esta unidad para una solicitud pendiente.
     *
     * @throws ConflictException si ya no está disponible
     */
    public void reserveForFinancing() {
        if (status != VehicleStatus.AVAILABLE) {
            throw new ConflictException(ErrorCode.VEHICLE_OUT_OF_STOCK,
                    "Vehicle is no longer available for financing");

        }
        status = VehicleStatus.RESERVED;

    }

    /**
     * Libera la unidad despues del rechazo o cancelacion de su solicitud.
     *
     * @throws ConflictException si no esta reservada
     */
    public void releaseFinancingReservation() {
        if (status != VehicleStatus.RESERVED) {
            throw new ConflictException(ErrorCode.VEHICLE_OUT_OF_STOCK,
                    "Vehicle has no active financing reservation");
        }
        status = VehicleStatus.AVAILABLE;
    }

    /**
     * Edita datos de catalogo solo mientras la unidad este disponible; conserva estado e identidad.
     */
    public void updateDetails(
            String vehicleType,
            String brand,
            String model,
            Integer manufacturingYear,
            String color,
            String chassisNumber,
            String engineNumber,
            String duaNumber,
            BigDecimal referencePrice) {

        if (status != VehicleStatus.AVAILABLE) {
            throw new ConflictException(ErrorCode.VEHICLE_NOT_EDITABLE,
                    "Only AVAILABLE vehicles can be updated");
        }

        Vehicle validated = create(
                vehicleCode,
                vehicleType,
                brand,
                model,
                manufacturingYear,
                color,
                chassisNumber,
                engineNumber,
                duaNumber,
                referencePrice,
                branchId
        );
        this.vehicleType = validated.vehicleType;
        this.brand = validated.brand;
        this.model = validated.model;
        this.manufacturingYear = validated.manufacturingYear;
        this.color = validated.color;
        this.chassisNumber = validated.chassisNumber;
        this.engineNumber = validated.engineNumber;
        this.duaNumber = validated.duaNumber;
        this.referencePrice = validated.referencePrice;
    }
}
