package pe.sotomayor.creditos.shared.error;

/**
 * Enumeracion que define los codigos de error utilizados por la aplicacion.
 *
 * <p>
 * Cada constante representa una condicion de negocio identificable por los
 * clientes de la API y constituye un contrato estable entre el dominio y la
 * capa de presentacion.
 * </p>
 *
 * <p>
 * Los codigos se agrupan por modulo para facilitar su mantenimiento y
 * evolucion.
 * </p>
 *
 * @author DarwinG
 */
public enum ErrorCode {

    // ==========================================================
    // SECURITY
    // ==========================================================

    USER_NOT_FOUND,

    USER_ALREADY_EXISTS,

    USER_INACTIVE,

    INVALID_CREDENTIALS,

    ROLE_NOT_FOUND,

    ROLE_ALREADY_EXISTS,

    ROLE_IN_USE,

    BRANCH_NOT_FOUND,

    BRANCH_ALREADY_EXISTS,

    BRANCH_IN_USE,

    BRANCH_INACTIVE,

    ACCOUNT_DISABLED,

    NEW_PASSWORD_MUST_BE_DIFFERENT,

    /**
     * La solicitud contiene un código de permiso ajeno al catálogo admitido.
     */
    INVALID_PERMISSION,
    /**
     * La operación dejaría el sistema sin un administrador de seguridad operativo.
     */
    LAST_SECURITY_ADMIN_REQUIRED,
    /**
     * El actor intentó desactivar el usuario con el que está operando.
     */
    SECURITY_SELF_DEACTIVATION,

    // ==========================================================
    // CUSTOMERS
    // ==========================================================

    CUSTOMER_NOT_FOUND,

    CUSTOMER_ALREADY_EXISTS,

    // ==========================================================
    // VEHICLES
    // ==========================================================

    VEHICLE_NOT_FOUND,

    VEHICLE_ALREADY_EXISTS,

    VEHICLE_OUT_OF_STOCK,

    VEHICLE_NOT_EDITABLE,

    // ==========================================================
    // CREDITS
    // ==========================================================

    // CreditApplication

    CREDIT_APPLICATION_INVALID_STATUS_TRANSITION,

    CREDIT_APPLICATION_RESOLUTION_DATE_REQUIRED,

    CREDIT_APPLICATION_RESOLVER_REQUIRED,

    CREDIT_APPLICATION_REJECTION_REASON_REQUIRED,

    CREDIT_APPLICATION_INVALID_DOWN_PAYMENT,

    CREDIT_APPLICATION_INVALID_FINANCED_AMOUNT,

    CREDIT_APPLICATION_INVALID_COMPENSATORY_INTEREST_RATE,

    CREDIT_APPLICATION_INVALID_INSTALLMENT_COUNT,

    CREDIT_APPLICATION_NOT_FOUND,

    // Credit

    CREDIT_NOT_FOUND,

    CREDIT_ALREADY_CANCELLED,

    CREDIT_ALREADY_FINISHED,

    // ==========================================================
    // INSTALLMENTS
    // ==========================================================

    INSTALLMENT_NOT_FOUND,

    INSTALLMENT_ALREADY_PAID,

    INSTALLMENT_INVALID_NUMBER,

    INSTALLMENT_INVALID_PRINCIPAL_AMOUNT,

    INSTALLMENT_INVALID_COMPENSATORY_INTEREST_AMOUNT,

    INSTALLMENT_INVALID_OUTSTANDING_PRINCIPAL,

    // ==========================================================
    // PAYMENTS
    // ==========================================================

    PAYMENT_NOT_FOUND,

    PAYMENT_ALREADY_APPLIED,

    // ==========================================================
    // CASH
    // ==========================================================

    CASH_TRANSACTION_NOT_FOUND,

    // ==========================================================
    // GENERALES
    // ==========================================================

    VALIDATION_ERROR,

    RESOURCE_NOT_FOUND,

    RESOURCE_ALREADY_EXISTS,

    UNAUTHORIZED,

    FORBIDDEN,

    /**
     * La escritura no pudo confirmarse porque otra transacción modificó el registro.
     */
    CONCURRENT_MODIFICATION,

    /**
     * Una restricción persistente impide completar la operación con los datos recibidos.
     */
    DATA_INTEGRITY_CONFLICT,

    INTERNAL_ERROR

}
