package pe.sotomayor.creditos.shared.exception;

import org.springframework.http.HttpStatus;
import pe.sotomayor.creditos.shared.error.ErrorCode;

/**
 * Excepción lanzada cuando una operación viola una regla de unicidad
 * o entra en conflicto con el estado actual del sistema.
 *
 * @author DarwinG
 */
public class ConflictException extends BusinessException {

    /**
     * Crea una excepción por conflicto de negocio.
     *
     * @param errorCode código del error
     * @param message   mensaje descriptivo
     */
    public ConflictException(
            ErrorCode errorCode,
            String message) {
        super(errorCode, HttpStatus.CONFLICT, message);
    }

    /**
     * Crea una excepción por conflicto de negocio
     * conservando la causa original.
     *
     * @param errorCode código del error
     * @param message   mensaje descriptivo
     * @param cause     causa original
     */
    public ConflictException(
            ErrorCode errorCode,
            String message,
            Throwable cause) {
        super(errorCode, HttpStatus.CONFLICT, message, cause);
    }

}
