package pe.sotomayor.creditos.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import pe.sotomayor.creditos.shared.error.ErrorCode;

/**
 * Excepción base para todas las reglas de negocio del sistema.
 *
 * <p>
 * Todas las excepciones funcionales deberán heredar de esta clase.
 * Proporciona un código de error estandarizado y el estado HTTP asociado,
 * permitiendo identificar de forma única la condición de negocio ocurrida
 * y construir respuestas consistentes desde la capa de presentación.
 * </p>
 *
 * <p>
 * Esta clase centraliza la información necesaria para que el
 * {@code GlobalExceptionHandler} genere respuestas uniformes,
 * evitando lógica condicional basada en el tipo de excepción.
 * </p>
 *
 * @author DarwinG
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    /**
     * Código único que identifica la condición de negocio.
     */
    private final ErrorCode errorCode;

    /**
     * Estado HTTP asociado a la excepción.
     */
    private final HttpStatus httpStatus;

    /**
     * Crea una nueva excepción de negocio.
     *
     * @param errorCode  código único del error
     * @param httpStatus estado HTTP asociado
     * @param message    mensaje descriptivo del error
     */
    protected BusinessException(
            ErrorCode errorCode,
            HttpStatus httpStatus,
            String message
    ) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * Crea una nueva excepción de negocio conservando la causa original.
     *
     * @param errorCode  código único del error
     * @param httpStatus estado HTTP asociado
     * @param message    mensaje descriptivo del error
     * @param cause      excepción original que originó el problema
     */
    protected BusinessException(
            ErrorCode errorCode,
            HttpStatus httpStatus,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

}
