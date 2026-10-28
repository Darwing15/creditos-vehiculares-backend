package pe.sotomayor.creditos.customers.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;

/**
 * Resultado minimo para autocompletar; no declara que la persona presente sea el titular.
 *
 * @author DarwinG
 */
@Schema(description = "Datos mínimos de DNI para autocompletar. No acredita titularidad ni crea una ficha comercial.")
public record IdentityLookupResponse(

        @Schema(
                description = "Tipo de documento consultado.",
                allowableValues = {"DNI"},
                example = "DNI",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        CustomerDocumentType documentType,

        @Schema(
                description = "DNI solicitado y contrastado con la respuesta externa.",
                type = "string",
                pattern = "[0-9]{8}",
                minLength = 8,
                maxLength = 8,
                example = "00000001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String documentNumber,

        @Schema(
                description = "Nombres normalizados; puede incluir más de un nombre.",
                minLength = 1,
                maxLength = 150,
                example = "NOMBRES",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String givenNames,

        @Schema(
                description = "Primer apellido disponible; puede ser null.",
                nullable = true,
                maxLength = 150,
                example = "PRUEBA",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String firstSurname,

        @Schema(
                description = "Segundo apellido disponible; puede ser null y no se inventa.",
                nullable = true,
                maxLength = 150,
                example = "EJEMPLO",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String secondSurname,

        @Schema(
                description = "Nombres seguidos de los apellidos presentes, normalizados y sin truncamiento silencioso.",
                minLength = 1,
                maxLength = 150,
                example = "NOMBRES PRUEBA",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String fullName,

        @Schema(
                description = "Proveedor original del dato, incluso cuando esta llamada responde desde caché.",
                allowableValues = {"FACTILIZA"},
                example = "FACTILIZA",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String provider,

        @Schema(
                description = "true si esta llamada reutilizó caché vigente; false si obtuvo un resultado externo. No indica identidad verificada.",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean fromCache,

        @Schema(
                description = "Instante de obtención serializado por JacksonConfig actual como segundos Unix, con fracción decimal para nanosegundos. No es la fecha de actualización de la fuente.",
                implementation = BigDecimal.class,
                type = "number",
                types = {"number"},
                example = "1793106000.000000000",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant consultedAt,

        @Schema(
                description = "Fin de vigencia local, posterior a consultedAt. Segundos Unix con fracción decimal; no inicia una purga automática.",
                implementation = BigDecimal.class,
                type = "number",
                types = {"number"},
                example = "1795698000.000000000",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant expiresAt
) {
}

