package pe.sotomayor.creditos.customers.domain.exception;

import org.springframework.http.HttpStatus;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.BusinessException;

/**
 * Fallo controlado de consulta documental; nunca transporta credenciales ni cuerpos externos.
 *
 * @author DarwinG
 */
public final class IdentityLookupException extends BusinessException {

    private IdentityLookupException(ErrorCode code, HttpStatus status, String message) {
        super(code, status, message);
    }

    /**
     * @return integracion externa deshabilitada; la cache vigente sigue disponible.
     */
    public static IdentityLookupException disabled() {
        return new IdentityLookupException(
                ErrorCode.IDENTITY_LOOKUP_DISABLED,
                HttpStatus.SERVICE_UNAVAILABLE,
                "La consulta de DNI no está habilitada.");
    }

    /**
     * @return proveedor o almacenamiento temporalmente indisponible.
     */
    public static IdentityLookupException unavailable() {
        return new IdentityLookupException(
                ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo consultar el DNI en este momento.");
    }

    /**
     * @return tiempo maximo de consulta agotado.
     */
    public static IdentityLookupException timeout() {
        return new IdentityLookupException(
                ErrorCode.IDENTITY_PROVIDER_TIMEOUT,
                HttpStatus.GATEWAY_TIMEOUT,
                "La consulta de DNI tardó demasiado.");
    }

    /**
     * @return respuesta externa incoherente; no debe persistirse.
     */
    public static IdentityLookupException invalidResponse() {
        return new IdentityLookupException(
                ErrorCode.IDENTITY_PROVIDER_INVALID_RESPONSE,
                HttpStatus.BAD_GATEWAY,
                "El servicio devolvió datos que no pudieron validarse.");
    }

    /**
     * @return ausencia de resultado informada explicitamente por el proveedor.
     */
    public static IdentityLookupException notFound() {
        return new IdentityLookupException(
                ErrorCode.IDENTITY_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "No se encontraron datos para este DNI.");
    }

    /**
     * @return presupuesto local de llamadas externas agotado.
     */
    public static IdentityLookupException quota() {
        return new IdentityLookupException(
                ErrorCode.IDENTITY_QUOTA_EXHAUSTED,
                HttpStatus.SERVICE_UNAVAILABLE,
                "Se alcanzó el límite configurado de consultas de DNI.");
    }

    /**
     * @return limite de frecuencia local o externo alcanzado.
     */
    public static IdentityLookupException rateLimited() {
        return new IdentityLookupException(
                ErrorCode.IDENTITY_RATE_LIMITED,
                HttpStatus.TOO_MANY_REQUESTS,
                "Espera un momento antes de volver a consultar el DNI.");
    }
}

