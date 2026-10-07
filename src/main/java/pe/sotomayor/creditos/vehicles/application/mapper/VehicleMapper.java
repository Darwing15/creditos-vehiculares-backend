package pe.sotomayor.creditos.vehicles.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.mapstruct.ReportingPolicy;

import pe.sotomayor.creditos.shared.concurrency.ResourceRevision;

import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleOptionResponse;
import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleResponse;

import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;

/**
 * Convierte Vehicle al contrato publico de lectura.
 *
 * @author DarwinG
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface VehicleMapper {
    /**
     * @param vehicle unidad de inventario.
     * @return DTO sin exponer entidad JPA.
     */
    VehicleOptionResponse toOption(Vehicle vehicle);

    /**
     * Convierte el detalle sin exponer la entidad JPA.
     */
    @Mapping(
            target = "revision",
            expression = "java(revision(vehicle))"
    )
    VehicleResponse toResponse(Vehicle vehicle);

    /**
     * Revision de los valores del registro, no de su representacion HTTP.
     */
    default String revision(Vehicle vehicle) {
        return ResourceRevision.of(
                vehicle.getVehicleCode(),
                vehicle.getBranchId(),
                vehicle.getStatus(),
                vehicle.getVehicleType(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getManufacturingYear(),
                vehicle.getColor(),
                vehicle.getChassisNumber(),
                vehicle.getEngineNumber(),
                vehicle.getDuaNumber(),
                vehicle.getReferencePrice()
        );
    }

}
