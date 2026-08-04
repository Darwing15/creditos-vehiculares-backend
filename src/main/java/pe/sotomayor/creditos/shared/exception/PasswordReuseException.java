package pe.sotomayor.creditos.shared.exception;

import org.springframework.http.HttpStatus;

import pe.sotomayor.creditos.shared.error.ErrorCode;

/**
 * Excepción lanzada cuando un usuario intenta reutilizar su
 * contraseña actual durante el proceso de cambio de contraseña.
 *
 * <p>
 * Esta excepción representa una regla de negocio del dominio de
 * seguridad que impide registrar una nueva contraseña idéntica a
 * la actualmente almacenada.
 * </p>
 *
 * <p>
 * Su propósito es preservar la integridad del proceso de cambio
 * de contraseña y promover el uso de nuevas credenciales por
 * parte del usuario.
 * </p>
 *
 * @author DarwinG
 */
public class PasswordReuseException extends BusinessException {

    /**
     * Crea una nueva excepción indicando que la nueva contraseña
     * coincide con la contraseña actualmente registrada.
     */
    public PasswordReuseException() {

        super(
                ErrorCode.NEW_PASSWORD_MUST_BE_DIFFERENT,
                HttpStatus.BAD_REQUEST,
                "New password must be different from current password."
        );
    }

}
