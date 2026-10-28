package pe.sotomayor.creditos.shared.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Representa la respuesta estandar de error devuelta por la API.
 *
 * <p>
 * Esta clase define el contrato de respuesta para todas las excepciones
 * controladas por la aplicacion, proporcionando informacion consistente
 * y uniforme a los consumidores de la API.
 * </p>
 *
 * @author DarwinG
 */
@Getter
@Builder
public class ApiError {

    /**
     * Fecha y hora en la que ocurrio el error.
     */
    @Schema(implementation = int[].class, type = "array", types = {"array"},
    description = "Fecha local serializada por JacksonConfig actual como un arreglo de cinco a siete enteros: [año, mes, día, hora, minuto, segundo opcional, nanosegundo opcional], sin zona horaria.")
    private final LocalDateTime timestamp;

    /**
     * Codigo de estado HTTP.
     */
    private final Integer status;

    /**
     * Nombre del estado HTTP.
     */
    private final String error;

    /**
     * Codigo de error propio de la aplicacion.
     */
    private final String errorCode;

    /**
     * Mensaje descriptivo del error.
     */
    private final String message;

    /**
     * Ruta del recurso solicitado.
     */
    private final String path;

}
