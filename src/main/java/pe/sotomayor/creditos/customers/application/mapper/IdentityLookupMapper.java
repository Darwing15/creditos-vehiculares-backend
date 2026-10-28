package pe.sotomayor.creditos.customers.application.mapper;

import java.time.Instant;
import java.time.Duration;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import pe.sotomayor.creditos.customers.domain.model.entity.IdentityDocumentCache;
import pe.sotomayor.creditos.customers.domain.model.valueobject.IdentityData;

import pe.sotomayor.creditos.customers.application.dto.response.IdentityLookupResponse;

/**
 * Conversiones explicitas que delegan invariantes a la fabrica del dominio.
 *
 * @author DarwinG
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface IdentityLookupMapper {
    /**
     * @return entidad auditada, sin crear una ficha Customer.
     */
    default IdentityDocumentCache toEntry(
            IdentityData data,
            Instant now,
            Duration ttl,
            Long actorId) {

        return IdentityDocumentCache.create(
                data, now, now.plus(ttl), actorId);
    }

    /**
     * @return contrato de consulta sin direccion, telefono ni datos financieros.
     */
    @Mapping(target = "documentType", constant = "DNI")
    @Mapping(target = "fromCache", source = "fromCache")
    IdentityLookupResponse toResponse(
            IdentityDocumentCache entry,
            boolean fromCache);

}
