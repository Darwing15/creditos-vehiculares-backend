package pe.sotomayor.creditos.credit.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;

import org.hibernate.type.SqlTypes;

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;
import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ValidationException;
import pe.sotomayor.creditos.shared.persistence.SoftDeleteEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Objects;

/**
 * Representa una solicitud de financiamiento presentada por un cliente.
 *
 * <p>Una solicitud permite registrar y conservar las condiciones financieras
 * sometidas a evaluación sin crear anticipadamente una obligación financiera.</p>
 *
 * <p>La aprobación de una solicitud no crea por sí misma un {@code Credit}.
 * La creación del crédito corresponde a un proceso posterior de formalización
 * contractual.</p>
 *
 * @author DarwinG
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "credit_applications")
@SQLRestriction("is_deleted = false")
public class CreditApplication extends SoftDeleteEntity {

    private static final int MIN_INSTALLMENT_COUNT = 1;

    private static final int MAX_INSTALLMENT_COUNT = 12;

    /**
     * Identificador público de la solicitud.
     */
    @Column(
            name = "application_code",
            nullable = false,
            unique = true
    )
    private UUID applicationCode;

    /**
     * Número visible utilizado para identificar comercialmente la solicitud.
     */
    @Column(
            name = "application_number",
            nullable = false,
            unique = true,
            length = 20
    )
    private String applicationNumber;

    /**
     * Moneda definida para toda la operación de financiamiento.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "currency",
            nullable = false,
            length = 3
    )
    private FinancingCurrency currency;

    /**
     * Precio de venta del vehículo conservado al registrar la solicitud.
     */
    @Column(
            name = "vehicle_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal vehiclePrice;

    /**
     * Cuota inicial considerada para la operación.
     */
    @Column(
            name = "down_payment",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal downPayment;

    /**
     * Monto efectivamente solicitado para financiamiento.
     *
     * <p>Corresponde al precio del vehículo menos la cuota inicial.</p>
     */
    @Column(
            name = "financed_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal financedAmount;

    /**
     * Tasa compensatoria mensual fijada para la solicitud.
     *
     * <p>Se almacena como porcentaje. Por ejemplo, 1.50 representa 1.50 %.</p>
     */
    @Column(
            name = "compensatory_interest_rate",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal compensatoryInterestRate;

    /**
     * Número de cuotas mensuales solicitadas para el financiamiento.
     */
    @Column(
            name = "installment_count",
            nullable = false
    )
    private Integer installmentCount;

    /**
     * Identificador interno del cliente solicitante.
     */
    @Column(
            name = "customer_id",
            nullable = false
    )
    private Long customerId;

    /**
     * Identificador interno del vehículo asociado a la solicitud.
     */
    @Column(
            name = "vehicle_id",
            nullable = false
    )
    private Long vehicleId;

    /**
     * Identificador interno de la sucursal donde se registra la solicitud.
     */
    @Column(
            name = "branch_id",
            nullable = false
    )
    private Long branchId;

    /**
     * Estado actual del ciclo de vida de la solicitud.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
            name = "status",
            nullable = false,
            columnDefinition = "credit_application_status"
    )
    private CreditApplicationStatus status;

    /**
     * Instante en el que la solicitud obtuvo una resolución terminal.
     *
     * <p>Permanece nulo mientras la solicitud se encuentre en estado
     * {@code PENDING}.</p>
     */
    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    /**
     * Identificador del usuario que produjo la resolución de la solicitud.
     *
     * <p>Puede permanecer nulo cuando la resolución haya sido realizada
     * automáticamente por el sistema.</p>
     */
    @Column(name = "resolved_by")
    private Long resolvedBy;

    /**
     * Motivo de rechazo de la solicitud.
     *
     * <p>Únicamente puede contener información cuando el estado sea
     * {@code REJECTED}.</p>
     */
    @Column(
            name = "rejection_reason",
            columnDefinition = "TEXT"
    )
    private String rejectionReason;

    /**
     * Crea una nueva solicitud de crédito en estado pendiente.
     *
     * <p>
     * La solicitud nace con su identidad pública, condiciones financieras
     * y referencias asociadas completamente definidas. La información de
     * resolución permanece vacía hasta que ocurra una transición terminal.
     * </p>
     *
     * @param applicationCode          código público único de la solicitud.
     * @param applicationNumber        número visible de la solicitud.
     * @param currency                 moneda de la operación.
     * @param vehiclePrice             precio del vehículo.
     * @param downPayment              cuota inicial.
     * @param financedAmount           monto financiado calculado.
     * @param compensatoryInterestRate tasa compensatoria mensual calculada.
     * @param installmentCount         número de cuotas.
     * @param customerId               identificador del cliente.
     * @param vehicleId                identificador del vehículo.
     * @param branchId                 identificador de la sucursal.
     * @return nueva solicitud de crédito pendiente.
     */
    public static CreditApplication create(
            UUID applicationCode,
            String applicationNumber,
            FinancingCurrency currency,
            BigDecimal vehiclePrice,
            BigDecimal downPayment,
            BigDecimal financedAmount,
            BigDecimal compensatoryInterestRate,
            Integer installmentCount,
            Long customerId,
            Long vehicleId,
            Long branchId) {

        validateRequiredFields(
                applicationCode,
                applicationNumber,
                currency,
                vehiclePrice,
                downPayment,
                financedAmount,
                compensatoryInterestRate,
                installmentCount,
                customerId,
                vehicleId,
                branchId
        );

        validateDownPayment(
                vehiclePrice,
                downPayment
        );

        validateFinancedAmount(
                vehiclePrice,
                downPayment,
                financedAmount
        );

        validateCompensatoryInterestRate(
                compensatoryInterestRate
        );

        validateInstallmentCount(
                installmentCount
        );

        CreditApplication application =
                new CreditApplication();

        application.applicationCode = applicationCode;
        application.applicationNumber = applicationNumber;
        application.currency = currency;
        application.vehiclePrice = vehiclePrice;
        application.downPayment = downPayment;
        application.financedAmount = financedAmount;
        application.compensatoryInterestRate =
                compensatoryInterestRate;
        application.installmentCount = installmentCount;
        application.customerId = customerId;
        application.vehicleId = vehicleId;
        application.branchId = branchId;

        application.status =
                CreditApplicationStatus.PENDING;

        application.resolvedAt = null;
        application.resolvedBy = null;
        application.rejectionReason = null;

        return application;
    }

    /**
     * Valida que los datos requeridos para crear la solicitud
     * de crédito se encuentren presentes.
     *
     * @param applicationCode          código público de la solicitud.
     * @param applicationNumber        número correlativo de la solicitud.
     * @param currency                 moneda del financiamiento.
     * @param vehiclePrice             precio de venta del vehículo.
     * @param downPayment              cuota inicial de la operación.
     * @param financedAmount           monto financiado.
     * @param compensatoryInterestRate tasa compensatoria mensual.
     * @param installmentCount         número de cuotas mensuales.
     * @param customerId               identificador del cliente.
     * @param vehicleId                identificador del vehículo.
     * @param branchId                 identificador de la sucursal.
     * @throws NullPointerException si alguno de los datos requeridos es nulo.
     */
    private static void validateRequiredFields(
            UUID applicationCode,
            String applicationNumber,
            FinancingCurrency currency,
            BigDecimal vehiclePrice,
            BigDecimal downPayment,
            BigDecimal financedAmount,
            BigDecimal compensatoryInterestRate,
            Integer installmentCount,
            Long customerId,
            Long vehicleId,
            Long branchId) {

        Objects.requireNonNull(
                applicationCode,
                "applicationCode must not be null"
        );

        Objects.requireNonNull(
                applicationNumber,
                "applicationNumber must not be null"
        );

        Objects.requireNonNull(
                currency,
                "currency must not be null"
        );

        Objects.requireNonNull(
                vehiclePrice,
                "vehiclePrice must not be null"
        );

        Objects.requireNonNull(
                downPayment,
                "downPayment must not be null"
        );

        Objects.requireNonNull(
                financedAmount,
                "financedAmount must not be null"
        );

        Objects.requireNonNull(
                compensatoryInterestRate,
                "compensatoryInterestRate must not be null"
        );

        Objects.requireNonNull(
                installmentCount,
                "installmentCount must not be null"
        );

        Objects.requireNonNull(
                customerId,
                "customerId must not be null"
        );

        Objects.requireNonNull(
                vehicleId,
                "vehicleId must not be null"
        );

        Objects.requireNonNull(
                branchId,
                "branchId must not be null"
        );
    }

    /**
     * Valida que la cuota inicial cumpla las condiciones
     * financieras necesarias para crear la solicitud.
     *
     * <p>
     * La cuota inicial debe ser mayor o igual a cero y menor
     * que el precio del vehículo.
     * </p>
     *
     * @param vehiclePrice precio de venta del vehículo.
     * @param downPayment  cuota inicial de la operación.
     * @throws ValidationException si la cuota inicial es negativa
     *                             o es mayor o igual al precio
     *                             del vehículo.
     */
    private static void validateDownPayment(
            BigDecimal vehiclePrice,
            BigDecimal downPayment) {

        if (downPayment.compareTo(BigDecimal.ZERO) < 0) {

            throw new ValidationException(
                    ErrorCode.CREDIT_APPLICATION_INVALID_DOWN_PAYMENT,
                    "Down payment cannot be negative"
            );
        }

        if (downPayment.compareTo(vehiclePrice) >= 0) {

            throw new ValidationException(
                    ErrorCode.CREDIT_APPLICATION_INVALID_DOWN_PAYMENT,
                    "Down payment must be lower than vehicle price"
            );
        }
    }

    /**
     * Valida que el monto financiado corresponda exactamente
     * al precio del vehículo menos la cuota inicial.
     *
     * @param vehiclePrice   precio de venta del vehículo.
     * @param downPayment    cuota inicial de la operación.
     * @param financedAmount monto financiado calculado.
     * @throws ValidationException si el monto financiado no coincide
     *                             con las condiciones financieras.
     */
    private static void validateFinancedAmount(
            BigDecimal vehiclePrice,
            BigDecimal downPayment,
            BigDecimal financedAmount) {

        BigDecimal expectedFinancedAmount =
                vehiclePrice.subtract(downPayment);

        if (financedAmount.compareTo(
                expectedFinancedAmount) != 0) {

            throw new ValidationException(
                    ErrorCode.CREDIT_APPLICATION_INVALID_FINANCED_AMOUNT,
                    "Financed amount must equal vehicle price minus down payment"
            );
        }
    }

    /**
     * Valida que la tasa compensatoria de la solicitud
     * no sea negativa.
     *
     * @param compensatoryInterestRate tasa compensatoria mensual.
     * @throws ValidationException si la tasa compensatoria es negativa.
     */
    private static void validateCompensatoryInterestRate(
            BigDecimal compensatoryInterestRate) {

        if (compensatoryInterestRate.compareTo(
                BigDecimal.ZERO) < 0) {

            throw new ValidationException(
                    ErrorCode.CREDIT_APPLICATION_INVALID_COMPENSATORY_INTEREST_RATE,
                    "Compensatory interest rate cannot be negative"
            );
        }
    }

    /**
     * Valida que el número de cuotas se encuentre dentro
     * del rango permitido para una solicitud de crédito.
     *
     * @param installmentCount número de cuotas mensuales solicitadas.
     * @throws ValidationException si el número de cuotas se encuentra
     *                             fuera del rango permitido.
     */
    private static void validateInstallmentCount(
            Integer installmentCount) {

        if (installmentCount < MIN_INSTALLMENT_COUNT
                || installmentCount > MAX_INSTALLMENT_COUNT) {

            throw new ValidationException(
                    ErrorCode.CREDIT_APPLICATION_INVALID_INSTALLMENT_COUNT,
                    "Installment count must be between 1 and 12"
            );
        }
    }

    /**
     * Aprueba la solicitud y registra la información de su resolución.
     *
     * @param resolvedAt instante en que se resolvió la solicitud
     * @param resolvedBy identificador del usuario responsable de la resolución
     */
    public void approve(
            OffsetDateTime resolvedAt,
            Long resolvedBy) {

        validateTransitionTo(CreditApplicationStatus.APPROVED);
        validateResolutionDate(resolvedAt);

        if (resolvedBy == null) {
            throw new ValidationException(
                    ErrorCode.CREDIT_APPLICATION_RESOLVER_REQUIRED,
                    "Resolver is required to approve a credit application"
            );
        }

        this.status = CreditApplicationStatus.APPROVED;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
        this.rejectionReason = null;
    }

    /**
     * Rechaza la solicitud y registra la información de su resolución.
     *
     * @param resolvedAt      instante en que se resolvió la solicitud
     * @param resolvedBy      identificador del usuario responsable de la resolución,
     *                        si corresponde
     * @param rejectionReason motivo del rechazo
     */
    public void reject(
            OffsetDateTime resolvedAt,
            Long resolvedBy,
            String rejectionReason) {

        validateTransitionTo(CreditApplicationStatus.REJECTED);
        validateResolutionDate(resolvedAt);
        validateRejectionReason(rejectionReason);

        this.status = CreditApplicationStatus.REJECTED;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
        this.rejectionReason = rejectionReason;
    }

    /**
     * Cancela la solicitud y registra la información de su resolución.
     *
     * @param resolvedAt instante en que se resolvió la solicitud
     * @param resolvedBy identificador del usuario responsable de la resolución,
     *                   si corresponde
     */
    public void cancel(
            OffsetDateTime resolvedAt,
            Long resolvedBy) {

        validateTransitionTo(CreditApplicationStatus.CANCELLED);
        validateResolutionDate(resolvedAt);

        this.status = CreditApplicationStatus.CANCELLED;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
    }

    /**
     * Marca la solicitud como expirada y registra la información de su resolución.
     *
     * @param resolvedAt instante en que se resolvió la solicitud
     * @param resolvedBy identificador del usuario responsable de la resolución,
     *                   si corresponde
     */
    public void expire(
            OffsetDateTime resolvedAt,
            Long resolvedBy) {

        validateTransitionTo(CreditApplicationStatus.EXPIRED);
        validateResolutionDate(resolvedAt);

        this.status = CreditApplicationStatus.EXPIRED;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
    }

    /**
     * Valida que el estado actual de la solicitud permita
     * la transición hacia el estado indicado.
     *
     * @param targetStatus estado destino de la transición
     * @throws ConflictException si la transición no está permitida
     */
    private void validateTransitionTo(
            CreditApplicationStatus targetStatus) {

        if (!status.canTransitionTo(targetStatus)) {
            throw new ConflictException(
                    ErrorCode.CREDIT_APPLICATION_INVALID_STATUS_TRANSITION,
                    "Credit application cannot transition from "
                            + status
                            + " to "
                            + targetStatus
            );
        }
    }

    /**
     * Valida que la resolución de la solicitud tenga una fecha asociada.
     *
     * @param resolvedAt instante en que se resolvió la solicitud
     * @throws ValidationException si la fecha de resolución no fue informada
     */
    private void validateResolutionDate(OffsetDateTime resolvedAt) {

        if (resolvedAt == null) {
            throw new ValidationException(
                    ErrorCode.CREDIT_APPLICATION_RESOLUTION_DATE_REQUIRED,
                    "Resolution date is required to resolve a credit application"
            );
        }
    }

    /**
     * Valida que el rechazo de la solicitud tenga un motivo informado.
     *
     * @param rejectionReason motivo del rechazo
     * @throws ValidationException si el motivo es nulo, vacío o contiene
     *                             únicamente espacios en blanco
     */
    private void validateRejectionReason(String rejectionReason) {

        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new ValidationException(
                    ErrorCode.CREDIT_APPLICATION_REJECTION_REASON_REQUIRED,
                    "Rejection reason is required to reject a credit application"
            );
        }
    }

}