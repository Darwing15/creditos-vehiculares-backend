package pe.sotomayor.creditos.customers.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import pe.sotomayor.creditos.customers.application.dto.response.CustomerOptionResponse;
import pe.sotomayor.creditos.customers.application.dto.response.CustomerResponse;

import pe.sotomayor.creditos.customers.domain.model.entity.Customer;
import pe.sotomayor.creditos.shared.concurrency.ResourceRevision;

/**
 * Convierte el agregado Customer al DTO de catalogo sin exponer la entidad.
 *
 * @author DarwinG
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CustomerMapper {
    /**
     * @param customer cliente de dominio.
     * @return opcion segura para el selector.
     */
    CustomerOptionResponse toOption(Customer customer);

    /**
     * Convierte el detalle sin exponer la entidad JPA.
     */
    @Mapping(
            target = "revision",
            expression = "java(revision(customer))"
    )
    CustomerResponse toResponse(Customer customer);

    /**
     * Revision de los valores del registro, no de su representacion HTTP.
     */
    default String revision(Customer customer) {
        return ResourceRevision.of(
                customer.getCustomerCode(),
                customer.getBranchId(),
                customer.getActive(),
                customer.getFullName(),
                customer.getDocumentType(),
                customer.getDocumentNumber(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getOccupation(),
                customer.getMonthlyIncome(),
                customer.getAddress(),
                customer.getMapReference()
        );
    }
}
