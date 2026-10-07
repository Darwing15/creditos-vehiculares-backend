package pe.sotomayor.creditos.credit.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.SQLRestriction;

import pe.sotomayor.creditos.credit.domain.model.enumeration.InstallmentPaymentStatus;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;
import pe.sotomayor.creditos.shared.persistence.SoftDeleteEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.Objects;

/**
 * Representa una cuota contractual perteneciente al cronograma
 * de pagos de un crédito formalizado.
 *
 * <p>Conserva la composición financiera y la fecha de vencimiento
 * determinadas al generar el cronograma. El importe contractual se
 * obtiene a partir del capital y del interés compensatorio, mientras
 * que toda nueva cuota inicia con un saldo pendiente igual a dicho
 * importe.</p>
 *
 * @author DarwinG
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "installments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_installments_credit_number",
                        columnNames = {
                                "credit_id",
                                "installment_number"
                        }
                )
        }
)
@SQLRestriction("is_deleted = false")
public class Installment extends SoftDeleteEntity {

    private static final int MIN_INSTALLMENT_NUMBER = 1;

    private static final int MAX_INSTALLMENT_NUMBER = 12;

    @Column(
            name = "installment_number",
            nullable = false
    )
    private Integer installmentNumber;

    @Column(
            name = "due_date",
            nullable = false
    )
    private LocalDate dueDate;

    @Column(
            name = "principal_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal principalAmount;

    @Column(
            name = "compensatory_interest_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal compensatoryInterestAmount;

    @Column(
            name = "amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "outstanding_principal",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal outstandingPrincipal;

    @Column(
            name = "balance",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal balance;

    @Column(
            name = "credit_id",
            nullable = false
    )
    private Long creditId;

    /**
     * Crea una nueva cuota contractual con sus condiciones iniciales.
     *
     * <p>El importe total se deriva de la suma del capital y del
     * interés compensatorio. El saldo pendiente inicia con el mismo
     * valor del importe contractual.</p>
     *
     * @param installmentNumber          número secuencial de la cuota
     * @param dueDate                    fecha contractual de vencimiento
     * @param principalAmount            capital asignado a la cuota
     * @param compensatoryInterestAmount interés compensatorio de la cuota
     * @param outstandingPrincipal       capital pendiente del crédito después de la cuota
     * @param creditId                   identificador interno del crédito
     * @return nueva cuota contractual
     */
    public static Installment create(
            Integer installmentNumber,
            LocalDate dueDate,
            BigDecimal principalAmount,
            BigDecimal compensatoryInterestAmount,
            BigDecimal outstandingPrincipal,
            Long creditId) {

        validateRequiredFields(
                installmentNumber,
                dueDate,
                principalAmount,
                compensatoryInterestAmount,
                outstandingPrincipal,
                creditId
        );

        validateInstallmentNumber(
                installmentNumber
        );

        validatePrincipalAmount(
                principalAmount
        );

        validateCompensatoryInterestAmount(
                compensatoryInterestAmount
        );

        validateOutstandingPrincipal(
                outstandingPrincipal
        );

        Installment installment =
                new Installment();

        installment.installmentNumber =
                installmentNumber;

        installment.dueDate =
                dueDate;

        installment.principalAmount =
                principalAmount;

        installment.compensatoryInterestAmount =
                compensatoryInterestAmount;

        installment.amount =
                principalAmount.add(
                        compensatoryInterestAmount
                );

        installment.outstandingPrincipal =
                outstandingPrincipal;

        installment.balance =
                installment.amount;

        installment.creditId =
                creditId;

        return installment;
    }

    private static void validateRequiredFields(
            Integer installmentNumber,
            LocalDate dueDate,
            BigDecimal principalAmount,
            BigDecimal compensatoryInterestAmount,
            BigDecimal outstandingPrincipal,
            Long creditId) {

        Objects.requireNonNull(
                installmentNumber,
                "Installment number is required"
        );

        Objects.requireNonNull(
                dueDate,
                "Due date is required"
        );

        Objects.requireNonNull(
                principalAmount,
                "Principal amount is required"
        );

        Objects.requireNonNull(
                compensatoryInterestAmount,
                "Compensatory interest amount is required"
        );

        Objects.requireNonNull(
                outstandingPrincipal,
                "Outstanding principal is required"
        );

        Objects.requireNonNull(
                creditId,
                "Credit ID is required"
        );
    }

    private static void validateInstallmentNumber(
            Integer installmentNumber) {

        if (installmentNumber < MIN_INSTALLMENT_NUMBER
                || installmentNumber > MAX_INSTALLMENT_NUMBER) {

            throw new ValidationException(
                    ErrorCode.INSTALLMENT_INVALID_NUMBER,
                    "Installment number must be between 1 and 12"
            );
        }
    }

    private static void validatePrincipalAmount(
            BigDecimal principalAmount) {

        if (principalAmount.signum() <= 0) {

            throw new ValidationException(
                    ErrorCode.INSTALLMENT_INVALID_PRINCIPAL_AMOUNT,
                    "Principal amount must be greater than zero"
            );
        }
    }

    private static void validateCompensatoryInterestAmount(
            BigDecimal compensatoryInterestAmount) {

        if (compensatoryInterestAmount.signum() < 0) {

            throw new ValidationException(
                    ErrorCode.INSTALLMENT_INVALID_COMPENSATORY_INTEREST_AMOUNT,
                    "Compensatory interest amount must not be negative"
            );
        }
    }

    private static void validateOutstandingPrincipal(
            BigDecimal outstandingPrincipal) {

        if (outstandingPrincipal.signum() < 0) {

            throw new ValidationException(
                    ErrorCode.INSTALLMENT_INVALID_OUTSTANDING_PRINCIPAL,
                    "Outstanding principal must not be negative"
            );
        }
    }

    /**
     * Obtiene el estado de pago actual de la cuota.
     *
     * @return estado de pago derivado de la cuota
     */
    public InstallmentPaymentStatus getPaymentStatus() {

        return InstallmentPaymentStatus.PENDING;
    }

    /**
     * Determina si la cuota se encuentra vencida en una fecha de evaluación.
     *
     * <p>Una cuota se considera vencida cuando su fecha contractual
     * de vencimiento es anterior a la fecha evaluada y conserva un
     * saldo pendiente mayor que cero.</p>
     *
     * @param evaluationDate fecha utilizada para evaluar el vencimiento
     * @return {@code true} cuando la cuota se encuentra vencida;
     * {@code false} en caso contrario
     */
    public boolean isOverdue(
            LocalDate evaluationDate) {

        return dueDate.isBefore(evaluationDate)
                && balance.signum() > 0;
    }
}
