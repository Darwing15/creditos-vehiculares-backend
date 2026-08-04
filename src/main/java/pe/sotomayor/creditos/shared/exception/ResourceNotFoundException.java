package pe.sotomayor.creditos.shared.exception;

import org.springframework.http.HttpStatus;
import pe.sotomayor.creditos.shared.error.ErrorCode;

/**
 * Excepción lanzada cuando un recurso solicitado no existe.
 *
 * <p>
 * Se utiliza cuando una entidad del dominio no puede ser encontrada
 * durante la ejecución de un caso de uso.
 * </p>
 *
 * @author DarwinG
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * Crea una excepción indicando que un recurso no fue encontrado.
     *
     * @param errorCode código del error
     * @param message   mensaje descriptivo
     */
    public ResourceNotFoundException(
            ErrorCode errorCode,
            String message) {
        super(errorCode, HttpStatus.NOT_FOUND, message);
    }

    /**
     * Crea una excepción indicando que un recurso no fue encontrado,
     * conservando la causa original.
     *
     * @param errorCode código del error
     * @param message   mensaje descriptivo
     * @param cause     causa original
     */
    public ResourceNotFoundException(
            ErrorCode errorCode,
            String message,
            Throwable cause) {
        super(errorCode, HttpStatus.NOT_FOUND, message, cause);
    }

}
