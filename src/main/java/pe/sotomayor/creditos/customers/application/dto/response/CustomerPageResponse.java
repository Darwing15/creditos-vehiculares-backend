package pe.sotomayor.creditos.customers.application.dto.response;

import java.util.List;

/**
 * Contrato estable del listado paginado, independiente de la serializacion de Spring Data.
 * @author DarwinG
 */
public record CustomerPageResponse(
        List<CustomerOptionResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}