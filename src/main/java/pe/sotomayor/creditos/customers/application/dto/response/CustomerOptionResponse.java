package pe.sotomayor.creditos.customers.application.dto.response;

import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;

import java.util.UUID;

/**
 * Datos minimos para seleccionar un cliente en la solicitud de credito.
 *
 * @author DarwinG
 */
public record CustomerOptionResponse(

        Long id,

        UUID customerCode,

        String fullName,

        CustomerDocumentType documentType,

        String documentNumber,

        String phone,

        String address,

        Long branchId,

        Boolean active
) {
}
