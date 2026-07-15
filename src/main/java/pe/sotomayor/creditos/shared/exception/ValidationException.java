package pe.sotomayor.creditos.shared.exception;

import org.springframework.http.HttpStatus;
import pe.sotomayor.creditos.shared.error.ErrorCode;

/**
 * Excepción lanzada cuando una validación de negocio no puede cumplirse.
 *
 * <p>
 * Representa reglas funcionales que impiden continuar con
 * la ejecución de un caso de uso.
 * </p>
 *
 * @author DarwinG
 */
public class ValidationException extends BusinessException {

    /**
     * Crea una excepción de validación.
     *
     * @param errorCode código del error
     * @param message   mensaje descriptivo
     */
    public ValidationException(
            ErrorCode errorCode,
            String message
    ) {
        super(errorCode, HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Crea una excepción de validación
     * conservando la causa original.
     *
     * @param errorCode código del error
     * @param message   mensaje descriptivo
     * @param cause     causa original
     */
    public ValidationException(
            ErrorCode errorCode,
            String message,
            Throwable cause
    ) {
        super(errorCode, HttpStatus.BAD_REQUEST, message, cause);
    }

}
