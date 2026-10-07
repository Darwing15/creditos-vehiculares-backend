package pe.sotomayor.creditos.credit.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.sotomayor.creditos.credit.domain.model.enumeration.InstallmentPaymentStatus;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstallmentTest {

    @Test
    void shouldCreateInstallmentWithValidContractualValues() {

        /* Arrange */

        Integer installmentNumber = 1;

        LocalDate dueDate =
                LocalDate.of(
                        2026,
                        10,
                        10
                );

        BigDecimal principalAmount =
                new BigDecimal("1000.00");

        BigDecimal compensatoryInterestAmount =
                new BigDecimal("150.00");

        BigDecimal outstandingPrincipal =
                new BigDecimal("9000.00");

        Long creditId = 1L;


        /* Act */

        Installment installment =
                Installment.create(
                        installmentNumber,
                        dueDate,
                        principalAmount,
                        compensatoryInterestAmount,
                        outstandingPrincipal,
                        creditId
                );


        /* Assert */

        assertThat(installment.getInstallmentNumber())
                .isEqualTo(1);

        assertThat(installment.getDueDate())
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                10,
                                10
                        )
                );

        assertThat(installment.getPrincipalAmount())
                .isEqualByComparingTo("1000.00");

        assertThat(installment.getCompensatoryInterestAmount())
                .isEqualByComparingTo("150.00");

        assertThat(installment.getAmount())
                .isEqualByComparingTo("1150.00");

        assertThat(installment.getOutstandingPrincipal())
                .isEqualByComparingTo("9000.00");

        assertThat(installment.getBalance())
                .isEqualByComparingTo("1150.00");

        assertThat(installment.getCreditId())
                .isEqualTo(1L);
    }

    @ParameterizedTest
    @EnumSource(RequiredField.class)
    void shouldRejectCreationWhenRequiredFieldIsNull(
            RequiredField requiredField) {

        /* Arrange */

        /* Act */

        /* Assert */

        assertThatThrownBy(
                () -> createInstallmentWithNullField(
                        requiredField
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage(
                        requiredField.message
                );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 13})
    void shouldRejectCreationWhenInstallmentNumberIsOutsideAllowedRange(
            int installmentNumber) {

        /* Arrange */

        LocalDate dueDate =
                LocalDate.of(
                        2026,
                        10,
                        10
                );

        BigDecimal principalAmount =
                new BigDecimal("1000.00");

        BigDecimal compensatoryInterestAmount =
                new BigDecimal("150.00");

        BigDecimal outstandingPrincipal =
                new BigDecimal("9000.00");

        Long creditId = 1L;


        /* Act */

        /* Assert */

        assertThatThrownBy(
                () -> Installment.create(
                        installmentNumber,
                        dueDate,
                        principalAmount,
                        compensatoryInterestAmount,
                        outstandingPrincipal,
                        creditId
                )
        )
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.INSTALLMENT_INVALID_NUMBER
                );
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 12})
    void shouldCreateInstallmentWhenInstallmentNumberIsAtAllowedBoundary(
            int installmentNumber) {

        /* Arrange */

        LocalDate dueDate =
                LocalDate.of(
                        2026,
                        10,
                        10
                );

        BigDecimal principalAmount =
                new BigDecimal("1000.00");

        BigDecimal compensatoryInterestAmount =
                new BigDecimal("150.00");

        BigDecimal outstandingPrincipal =
                new BigDecimal("9000.00");

        Long creditId = 1L;

        /* Act */

        Installment installment =
                Installment.create(
                        installmentNumber,
                        dueDate,
                        principalAmount,
                        compensatoryInterestAmount,
                        outstandingPrincipal,
                        creditId
                );

        /* Assert */

        assertThat(installment.getInstallmentNumber())
                .isEqualTo(installmentNumber);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-0.01"})
    void shouldRejectCreationWhenPrincipalAmountIsNotPositive(
            String principalAmountValue) {

        /* Arrange */

        Integer installmentNumber = 1;

        LocalDate dueDate =
                LocalDate.of(
                        2026,
                        10,
                        10
                );

        BigDecimal principalAmount =
                new BigDecimal(
                        principalAmountValue
                );

        BigDecimal compensatoryInterestAmount =
                new BigDecimal("150.00");

        BigDecimal outstandingPrincipal =
                new BigDecimal("9000.00");

        Long creditId = 1L;

        /* Act */

        /* Assert */

        assertThatThrownBy(
                () -> Installment.create(
                        installmentNumber,
                        dueDate,
                        principalAmount,
                        compensatoryInterestAmount,
                        outstandingPrincipal,
                        creditId
                )
        )
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.INSTALLMENT_INVALID_PRINCIPAL_AMOUNT
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "-1.00"})
    void shouldRejectCreationWhenCompensatoryInterestAmountIsNegative(
            String compensatoryInterestAmountValue) {

        /* Arrange */

        Integer installmentNumber = 1;

        LocalDate dueDate =
                LocalDate.of(
                        2026,
                        10,
                        10
                );

        BigDecimal principalAmount =
                new BigDecimal("1000.00");

        BigDecimal compensatoryInterestAmount =
                new BigDecimal(
                        compensatoryInterestAmountValue
                );

        BigDecimal outstandingPrincipal =
                new BigDecimal("9000.00");

        Long creditId = 1L;

        /* Act */

        /* Assert */

        assertThatThrownBy(
                () -> Installment.create(
                        installmentNumber,
                        dueDate,
                        principalAmount,
                        compensatoryInterestAmount,
                        outstandingPrincipal,
                        creditId
                )
        )
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.INSTALLMENT_INVALID_COMPENSATORY_INTEREST_AMOUNT
                );
    }

    @Test
    void shouldCreateInstallmentWhenCompensatoryInterestAmountIsZero() {

        /* Arrange */

        Integer installmentNumber = 1;

        LocalDate dueDate =
                LocalDate.of(
                        2026,
                        10,
                        10
                );

        BigDecimal principalAmount =
                new BigDecimal("1000.00");

        BigDecimal compensatoryInterestAmount =
                new BigDecimal("0.00");

        BigDecimal outstandingPrincipal =
                new BigDecimal("9000.00");

        Long creditId = 1L;

        /* Act */

        Installment installment =
                Installment.create(
                        installmentNumber,
                        dueDate,
                        principalAmount,
                        compensatoryInterestAmount,
                        outstandingPrincipal,
                        creditId
                );

        /* Assert */

        assertThat(installment.getCompensatoryInterestAmount())
                .isEqualByComparingTo("0.00");

        assertThat(installment.getAmount())
                .isEqualByComparingTo("1000.00");

        assertThat(installment.getBalance())
                .isEqualByComparingTo("1000.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "-1.00"})
    void shouldRejectCreationWhenOutstandingPrincipalIsNegative(
            String outstandingPrincipalValue) {

        /* Arrange */

        Integer installmentNumber = 1;

        LocalDate dueDate =
                LocalDate.of(
                        2026,
                        10,
                        10
                );

        BigDecimal principalAmount =
                new BigDecimal("1000.00");

        BigDecimal compensatoryInterestAmount =
                new BigDecimal("150.00");

        BigDecimal outstandingPrincipal =
                new BigDecimal(
                        outstandingPrincipalValue
                );

        Long creditId = 1L;

        /* Act */

        /* Assert */

        assertThatThrownBy(
                () -> Installment.create(
                        installmentNumber,
                        dueDate,
                        principalAmount,
                        compensatoryInterestAmount,
                        outstandingPrincipal,
                        creditId
                )
        )
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.INSTALLMENT_INVALID_OUTSTANDING_PRINCIPAL
                );
    }

    @Test
    void shouldCreateInstallmentWhenOutstandingPrincipalIsZero() {

        /* Arrange */

        Integer installmentNumber = 12;

        LocalDate dueDate =
                LocalDate.of(
                        2027,
                        9,
                        10
                );

        BigDecimal principalAmount =
                new BigDecimal("1000.00");

        BigDecimal compensatoryInterestAmount =
                new BigDecimal("150.00");

        BigDecimal outstandingPrincipal =
                new BigDecimal("0.00");

        Long creditId = 1L;

        /* Act */

        Installment installment =
                Installment.create(
                        installmentNumber,
                        dueDate,
                        principalAmount,
                        compensatoryInterestAmount,
                        outstandingPrincipal,
                        creditId
                );

        /* Assert */

        assertThat(installment.getOutstandingPrincipal())
                .isEqualByComparingTo("0.00");
    }

    @Test
    void shouldReturnPendingPaymentStatusWhenInstallmentHasNotReceivedPayments() {

        /* Arrange */

        Installment installment = createInstallment();

        /* Act */

        InstallmentPaymentStatus paymentStatus =
                installment.getPaymentStatus();

        /* Assert */

        assertThat(paymentStatus)
                .isEqualTo(
                        InstallmentPaymentStatus.PENDING
                );
    }

    @Test
    void shouldReturnTrueWhenInstallmentIsOverdue() {

        /* Arrange */

        Installment installment = createInstallment();

        LocalDate evaluationDate =
                LocalDate.of(
                        2026,
                        10,
                        11
                );

        /* Act */

        boolean overdue =
                installment.isOverdue(
                        evaluationDate
                );

        /* Assert */

        assertThat(overdue)
                .isTrue();
    }

    @Test
    void shouldReturnFalseWhenInstallmentIsDueOnEvaluationDate() {

        /* Arrange */

        Installment installment = createInstallment();

        LocalDate evaluationDate =
                LocalDate.of(
                        2026,
                        10,
                        10
                );

        /* Act */

        boolean overdue =
                installment.isOverdue(
                        evaluationDate
                );

        /* Assert */

        assertThat(overdue)
                .isFalse();
    }

    @Test
    void shouldReturnFalseWhenInstallmentIsNotYetDue() {

        /* Arrange */

        Installment installment = createInstallment();

        LocalDate evaluationDate =
                LocalDate.of(
                        2026,
                        10,
                        9
                );

        /* Act */

        boolean overdue =
                installment.isOverdue(
                        evaluationDate
                );

        /* Assert */

        assertThat(overdue)
                .isFalse();
    }

    private Installment createInstallment() {

        return Installment.create(
                1,
                LocalDate.of(
                        2026,
                        10,
                        10
                ),
                new BigDecimal("1000.00"),
                new BigDecimal("150.00"),
                new BigDecimal("9000.00"),
                1L
        );
    }

    private Installment createInstallmentWithNullField(
            RequiredField requiredField) {

        Integer installmentNumber =
                requiredField == RequiredField.INSTALLMENT_NUMBER
                        ? null
                        : 1;

        LocalDate dueDate =
                requiredField == RequiredField.DUE_DATE
                        ? null
                        : LocalDate.of(
                        2026,
                        10,
                        10
                );

        BigDecimal principalAmount =
                requiredField == RequiredField.PRINCIPAL_AMOUNT
                        ? null
                        : new BigDecimal("1000.00");

        BigDecimal compensatoryInterestAmount =
                requiredField == RequiredField.COMPENSATORY_INTEREST_AMOUNT
                        ? null
                        : new BigDecimal("150.00");

        BigDecimal outstandingPrincipal =
                requiredField == RequiredField.OUTSTANDING_PRINCIPAL
                        ? null
                        : new BigDecimal("9000.00");

        Long creditId =
                requiredField == RequiredField.CREDIT_ID
                        ? null
                        : 1L;

        return Installment.create(
                installmentNumber,
                dueDate,
                principalAmount,
                compensatoryInterestAmount,
                outstandingPrincipal,
                creditId
        );
    }

    private enum RequiredField {

        INSTALLMENT_NUMBER(
                "Installment number is required"
        ),

        DUE_DATE(
                "Due date is required"
        ),

        PRINCIPAL_AMOUNT(
                "Principal amount is required"
        ),

        COMPENSATORY_INTEREST_AMOUNT(
                "Compensatory interest amount is required"
        ),

        OUTSTANDING_PRINCIPAL(
                "Outstanding principal is required"
        ),

        CREDIT_ID(
                "Credit ID is required"
        );

        private final String message;

        RequiredField(String message) {
            this.message = message;
        }
    }
}
