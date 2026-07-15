package pe.sotomayor.creditos.shared.exception;

import org.springframework.http.HttpStatus;
import pe.sotomayor.creditos.shared.error.ErrorCode;

/**
 * Excepción lanzada cuando un usuario intenta acceder
 * a un recurso sin estar autenticado o autorizado.
 *
 * @author DarwinG
 */
public class UnauthorizedException extends BusinessException {

    /**
     * Crea una excepción de autorización.
     *
     * @param errorCode código del error
     * @param message   mensaje descriptivo
     */
    public UnauthorizedException(
            ErrorCode errorCode,
            String message
    ) {
        super(errorCode, HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * Crea una excepción de autorización
     * conservando la causa original.
     *
     * @param errorCode código del error
     * @param message   mensaje descriptivo
     * @param cause     causa original
     */
    public UnauthorizedException(
            ErrorCode errorCode,
            String message,
            Throwable cause
    ) {
        super(errorCode, HttpStatus.UNAUTHORIZED, message, cause);
    }

}
