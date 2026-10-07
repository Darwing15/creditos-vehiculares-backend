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

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditStatus;
import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import pe.sotomayor.creditos.shared.persistence.SoftDeleteEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representa una obligación financiera formalmente otorgada
 * a un cliente para el financiamiento de un vehículo.
 *
 * <p>
 * Un crédito únicamente puede existir como consecuencia de una
 * solicitud de crédito previamente aprobada y después de haberse
 * formalizado contractualmente la operación.
 * </p>
 *
 * <p>
 * La entidad conserva las condiciones financieras formalizadas
 * para la operación y representa la evolución financiera de la
 * obligación durante su ciclo de vida.
 * </p>
 *
 * @author DarwinG
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "credits")
@SQLRestriction("is_deleted = false")
public class Credit extends SoftDeleteEntity {

    /**
     * Código público único del crédito.
     */
    @Column(
            name = "credit_code",
            nullable = false,
            unique = true
    )
    private UUID creditCode;

    /**
     * Número visible utilizado para identificar comercialmente el crédito.
     */
    @Column(
            name = "credit_number",
            nullable = false,
            unique = true,
            length = 20
    )
    private String creditNumber;

    /**
     * Identificador de la solicitud de crédito que originó
     * la obligación financiera.
     *
     * <p>
     * Cada crédito pertenece exactamente a una solicitud aprobada
     * y una solicitud puede originar como máximo un crédito.
     * </p>
     */
    @Column(
            name = "credit_application_id",
            nullable = false,
            unique = true
    )
    private Long creditApplicationId;

    /**
     * Moneda contractual de la operación financiera.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "currency",
            nullable = false,
            length = 3
    )
    private FinancingCurrency currency;

    /**
     * Precio de venta del vehículo conservado como condición
     * contractual del financiamiento.
     */
    @Column(
            name = "vehicle_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal vehiclePrice;

    /**
     * Cuota inicial correspondiente a la operación formalizada.
     */
    @Column(
            name = "down_payment",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal downPayment;

    /**
     * Monto efectivamente financiado.
     *
     * <p>
     * Corresponde al precio de venta del vehículo menos
     * la cuota inicial.
     * </p>
     */
    @Column(
            name = "financed_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal financedAmount;

    /**
     * Tasa compensatoria mensual formalizada para el crédito.
     *
     * <p>
     * Se almacena como porcentaje. Por ejemplo, 1.50 representa 1.50 %.
     * </p>
     */
    @Column(
            name = "compensatory_interest_rate",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal compensatoryInterestRate;

    /**
     * Tasa moratoria contractual fijada para el crédito.
     *
     * <p>
     * El cálculo y aplicación de la mora corresponde al proceso
     * específico de cuotas y cobranza.
     * </p>
     */
    @Column(
            name = "moratory_interest_rate",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal moratoryInterestRate;

    /**
     * Número de cuotas mensuales pactadas para el financiamiento.
     */
    @Column(
            name = "installment_count",
            nullable = false
    )
    private Integer installmentCount;

    /**
     * Instante en que se formalizó la obligación mediante
     * la firma del contrato de financiamiento.
     */
    @Column(
            name = "contract_signed_at",
            nullable = false
    )
    private OffsetDateTime contractSignedAt;

    /**
     * Identificador interno del cliente titular del crédito.
     */
    @Column(
            name = "customer_id",
            nullable = false
    )
    private Long customerId;

    /**
     * Identificador interno del vehículo financiado.
     */
    @Column(
            name = "vehicle_id",
            nullable = false
    )
    private Long vehicleId;

    /**
     * Identificador interno de la sucursal asociada al crédito.
     */
    @Column(
            name = "branch_id",
            nullable = false
    )
    private Long branchId;

    /**
     * Estado financiero actual de la obligación.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
            name = "status",
            nullable = false,
            columnDefinition = "credit_status"
    )
    private CreditStatus status;

}