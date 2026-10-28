package pe.sotomayor.creditos.customers.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;

/**
 * Documento para consulta; no admite datos del actor, sucursal ni proveedor.
 *
 * @author DarwinG
 */
@Schema(description = "Solicitud de consulta documental independiente del alta de clientes. Solo admite DNI.")
public record IdentityLookupRequest(

        @Schema(
                description = "Único tipo habilitado para esta consulta; CE se rechaza en el caso de uso.",
                allowableValues = {"DNI"},
                example = "DNI",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        CustomerDocumentType documentType,

        @Schema(
                description = "Ocho dígitos ASCII sin espacios. Se conserva como texto para mantener ceros iniciales.",
                type = "string",
                pattern = "[0-9]{8}",
                minLength = 8,
                maxLength = 8,
                example = "00000001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Pattern(regexp = "[0-9]{8}")
        String documentNumber
) {
}

