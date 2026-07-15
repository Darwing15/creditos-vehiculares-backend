package pe.sotomayor.creditos.shared.error;

/**
 * Enumeración que define los códigos de error utilizados por la aplicación.
 *
 * <p>
 * Cada constante representa una condición de negocio identificable por los
 * clientes de la API y constituye un contrato estable entre el dominio y la
 * capa de presentación.
 * </p>
 *
 * <p>
 * Los códigos se agrupan por módulo para facilitar su mantenimiento y
 * evolución.
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

    // ==========================================================
    // CREDITS
    // ==========================================================

    CREDIT_NOT_FOUND,

    CREDIT_ALREADY_CANCELLED,

    CREDIT_ALREADY_FINISHED,

    // ==========================================================
    // INSTALLMENTS
    // ==========================================================

    INSTALLMENT_NOT_FOUND,

    INSTALLMENT_ALREADY_PAID,

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

    INTERNAL_ERROR

}
