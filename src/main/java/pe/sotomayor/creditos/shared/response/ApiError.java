package pe.sotomayor.creditos.shared.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Representa la respuesta estándar de error devuelta por la API.
 *
 * <p>
 * Esta clase define el contrato de respuesta para todas las excepciones
 * controladas por la aplicación, proporcionando información consistente
 * y uniforme a los consumidores de la API.
 * </p>
 *
 * @author DarwinG
 */
@Getter
@Builder
public class ApiError {

    /**
     * Fecha y hora en la que ocurrió el error.
     */
    private final LocalDateTime timestamp;

    /**
     * Código de estado HTTP.
     */
    private final Integer status;

    /**
     * Nombre del estado HTTP.
     */
    private final String error;

    /**
     * Código de error propio de la aplicación.
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
