package pe.sotomayor.creditos.credit.application.mapper.creditapplication;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.domain.model.entity.CreditApplication;

/**
 * Realiza la conversión entre el agregado
 * {@link CreditApplication} y sus DTOs de respuesta.
 *
 * <p>
 * La creación del agregado no corresponde al mapper.
 * Las solicitudes de crédito son construidas mediante
 * la API de dominio definida por {@link CreditApplication},
 * preservando sus invariantes desde el nacimiento.
 * </p>
 *
 * @author DarwinG
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CreditApplicationMapper {

    /**
     * Convierte una solicitud de crédito en su representación
     * de respuesta.
     *
     * @param application solicitud de crédito.
     * @return información de la solicitud.
     */
    CreditApplicationResponse toResponse(
            CreditApplication application
    );

}
