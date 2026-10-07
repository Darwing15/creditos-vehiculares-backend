package pe.sotomayor.creditos.customers.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;

/**
 * Detalle de la ficha y revision para su actualizacion.
 *
 * @author DarwinG
 */
public record CustomerResponse(

        Long id,

        UUID customerCode,

        String fullName,

        CustomerDocumentType documentType,

        String documentNumber,

        String phone,

        String email,

        String occupation,

        BigDecimal monthlyIncome,

        String address,

        String mapReference,

        Long branchId,

        Boolean active,

        String revision
) {
}
