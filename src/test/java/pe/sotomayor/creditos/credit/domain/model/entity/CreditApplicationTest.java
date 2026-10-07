package pe.sotomayor.creditos.credit.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;
import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CreditApplicationTest {

    @Test
    void shouldCreatePendingCreditApplication() {

        /* Arrange */

        UUID applicationCode =
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                );

        /* Act */

        CreditApplication application =
                CreditApplication.create(
                        applicationCode,
                        "CA-00000001",
                        FinancingCurrency.PEN,
                        new BigDecimal("12000.00"),
                        new BigDecimal("2000.00"),
                        new BigDecimal("10000.00"),
                        new BigDecimal("1.50"),
                        12,
                        1L,
                        2L,
                        3L
                );

        /* Assert */

        assertThat(application.getApplicationCode())
                .isEqualTo(applicationCode);

        assertThat(application.getApplicationNumber())
                .isEqualTo("CA-00000001");

        assertThat(application.getCurrency())
                .isEqualTo(FinancingCurrency.PEN);

        assertThat(application.getVehiclePrice())
                .isEqualByComparingTo("12000.00");

        assertThat(application.getDownPayment())
                .isEqualByComparingTo("2000.00");

        assertThat(application.getFinancedAmount())
                .isEqualByComparingTo("10000.00");

        assertThat(application.getCompensatoryInterestRate())
                .isEqualByComparingTo("1.50");

        assertThat(application.getInstallmentCount())
                .isEqualTo(12);

        assertThat(application.getCustomerId())
                .isEqualTo(1L);

        assertThat(application.getVehicleId())
                .isEqualTo(2L);

        assertThat(application.getBranchId())
                .isEqualTo(3L);

        assertThat(application.getStatus())
                .isEqualTo(
                        CreditApplicationStatus.PENDING
                );

        assertThat(application.getResolvedAt())
                .isNull();

        assertThat(application.getResolvedBy())
                .isNull();

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldApprovePendingApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();
        OffsetDateTime resolvedAt = createResolvedAt();
        Long resolvedBy = 1L;

        /* Act */
        application.approve(resolvedAt, resolvedBy);

        /* Assert */
        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.APPROVED);

        assertThat(application.getResolvedAt())
                .isEqualTo(resolvedAt);

        assertThat(application.getResolvedBy())
                .isEqualTo(resolvedBy);

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldThrowConflictExceptionWhenApprovingAlreadyApprovedApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();
        OffsetDateTime firstResolutionAt = createResolvedAt();
        Long firstResolvedBy = 1L;

        application.approve(
                firstResolutionAt,
                firstResolvedBy
        );

        OffsetDateTime secondResolutionAt =
                firstResolutionAt.plusHours(1);

        Long secondResolvedBy = 2L;

        /* Act */
        Throwable throwable = catchThrowable(() ->
                application.approve(
                        secondResolutionAt,
                        secondResolvedBy
                )
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_INVALID_STATUS_TRANSITION
                );

        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.APPROVED);

        assertThat(application.getResolvedAt())
                .isEqualTo(firstResolutionAt);

        assertThat(application.getResolvedBy())
                .isEqualTo(firstResolvedBy);

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldRequireResolutionDateWhenApprovingApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();
        Long resolvedBy = 1L;

        /* Act */
        Throwable throwable = catchThrowable(() ->
                application.approve(
                        null,
                        resolvedBy
                )
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_RESOLUTION_DATE_REQUIRED
                );

        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.PENDING);

        assertThat(application.getResolvedAt())
                .isNull();

        assertThat(application.getResolvedBy())
                .isNull();

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldRequireResolverWhenApprovingApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();
        OffsetDateTime resolvedAt = createResolvedAt();

        /* Act */
        Throwable throwable = catchThrowable(() ->
                application.approve(
                        resolvedAt,
                        null
                )
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_RESOLVER_REQUIRED
                );

        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.PENDING);

        assertThat(application.getResolvedAt())
                .isNull();

        assertThat(application.getResolvedBy())
                .isNull();

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldRejectPendingApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();
        OffsetDateTime resolvedAt = createResolvedAt();
        Long resolvedBy = 1L;
        String rejectionReason = "Customer does not meet approval criteria";

        /* Act */
        application.reject(
                resolvedAt,
                resolvedBy,
                rejectionReason
        );

        /* Assert */
        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.REJECTED);

        assertThat(application.getResolvedAt())
                .isEqualTo(resolvedAt);

        assertThat(application.getResolvedBy())
                .isEqualTo(resolvedBy);

        assertThat(application.getRejectionReason())
                .isEqualTo(rejectionReason);
    }

    @Test
    void shouldThrowConflictExceptionWhenRejectingAlreadyApprovedApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();

        OffsetDateTime firstResolutionAt = createResolvedAt();
        Long firstResolvedBy = 1L;

        application.approve(
                firstResolutionAt,
                firstResolvedBy
        );

        OffsetDateTime secondResolutionAt =
                firstResolutionAt.plusHours(1);

        Long secondResolvedBy = 2L;
        String rejectionReason = "Customer does not meet approval criteria";

        /* Act */
        Throwable throwable = catchThrowable(() ->
                application.reject(
                        secondResolutionAt,
                        secondResolvedBy,
                        rejectionReason
                )
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_INVALID_STATUS_TRANSITION
                );

        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.APPROVED);

        assertThat(application.getResolvedAt())
                .isEqualTo(firstResolutionAt);

        assertThat(application.getResolvedBy())
                .isEqualTo(firstResolvedBy);

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldRequireResolutionDateWhenRejectingApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();
        Long resolvedBy = 1L;
        String rejectionReason = "Customer does not meet approval criteria";

        /* Act */
        Throwable throwable = catchThrowable(() ->
                application.reject(
                        null,
                        resolvedBy,
                        rejectionReason
                )
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_RESOLUTION_DATE_REQUIRED
                );

        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.PENDING);

        assertThat(application.getResolvedAt())
                .isNull();

        assertThat(application.getResolvedBy())
                .isNull();

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldRequireRejectionReasonWhenRejectingApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();
        OffsetDateTime resolvedAt = createResolvedAt();
        Long resolvedBy = 1L;

        /* Act */
        Throwable throwable = catchThrowable(() ->
                application.reject(
                        resolvedAt,
                        resolvedBy,
                        null
                )
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_REJECTION_REASON_REQUIRED
                );

        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.PENDING);

        assertThat(application.getResolvedAt())
                .isNull();

        assertThat(application.getResolvedBy())
                .isNull();

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldRequireNonBlankRejectionReasonWhenRejectingApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();
        OffsetDateTime resolvedAt = createResolvedAt();
        Long resolvedBy = 1L;
        String rejectionReason = "   ";

        /* Act */
        Throwable throwable = catchThrowable(() ->
                application.reject(
                        resolvedAt,
                        resolvedBy,
                        rejectionReason
                )
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_REJECTION_REASON_REQUIRED
                );

        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.PENDING);

        assertThat(application.getResolvedAt())
                .isNull();

        assertThat(application.getResolvedBy())
                .isNull();

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldCancelPendingApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();
        OffsetDateTime resolvedAt = createResolvedAt();
        Long resolvedBy = 1L;

        /* Act */
        application.cancel(
                resolvedAt,
                resolvedBy
        );

        /* Assert */
        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.CANCELLED);

        assertThat(application.getResolvedAt())
                .isEqualTo(resolvedAt);

        assertThat(application.getResolvedBy())
                .isEqualTo(resolvedBy);

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldThrowConflictExceptionWhenCancellingAlreadyApprovedApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();

        OffsetDateTime firstResolutionAt = createResolvedAt();
        Long firstResolvedBy = 1L;

        application.approve(
                firstResolutionAt,
                firstResolvedBy
        );

        OffsetDateTime secondResolutionAt =
                firstResolutionAt.plusHours(1);

        Long secondResolvedBy = 2L;

        /* Act */
        Throwable throwable = catchThrowable(() ->
                application.cancel(
                        secondResolutionAt,
                        secondResolvedBy
                )
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_INVALID_STATUS_TRANSITION
                );

        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.APPROVED);

        assertThat(application.getResolvedAt())
                .isEqualTo(firstResolutionAt);

        assertThat(application.getResolvedBy())
                .isEqualTo(firstResolvedBy);

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldRequireResolutionDateWhenCancellingApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();
        Long resolvedBy = 1L;

        /* Act */
        Throwable throwable = catchThrowable(() ->
                application.cancel(
                        null,
                        resolvedBy
                )
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_RESOLUTION_DATE_REQUIRED
                );

        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.PENDING);

        assertThat(application.getResolvedAt())
                .isNull();

        assertThat(application.getResolvedBy())
                .isNull();

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldExpirePendingApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();
        OffsetDateTime resolvedAt = createResolvedAt();
        Long resolvedBy = 1L;

        /* Act */
        application.expire(
                resolvedAt,
                resolvedBy
        );

        /* Assert */
        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.EXPIRED);

        assertThat(application.getResolvedAt())
                .isEqualTo(resolvedAt);

        assertThat(application.getResolvedBy())
                .isEqualTo(resolvedBy);

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldThrowConflictExceptionWhenExpiringAlreadyApprovedApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();

        OffsetDateTime firstResolutionAt = createResolvedAt();
        Long firstResolvedBy = 1L;

        application.approve(
                firstResolutionAt,
                firstResolvedBy
        );

        OffsetDateTime secondResolutionAt =
                firstResolutionAt.plusHours(1);

        Long secondResolvedBy = 2L;

        /* Act */
        Throwable throwable = catchThrowable(() ->
                application.expire(
                        secondResolutionAt,
                        secondResolvedBy
                )
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_INVALID_STATUS_TRANSITION
                );

        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.APPROVED);

        assertThat(application.getResolvedAt())
                .isEqualTo(firstResolutionAt);

        assertThat(application.getResolvedBy())
                .isEqualTo(firstResolvedBy);

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldRequireResolutionDateWhenExpiringApplication() {

        /* Arrange */
        CreditApplication application = createCreditApplication();
        Long resolvedBy = 1L;

        /* Act */
        Throwable throwable = catchThrowable(() ->
                application.expire(
                        null,
                        resolvedBy
                )
        );

        /* Assert */
        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_RESOLUTION_DATE_REQUIRED
                );

        assertThat(application.getStatus())
                .isEqualTo(CreditApplicationStatus.PENDING);

        assertThat(application.getResolvedAt())
                .isNull();

        assertThat(application.getResolvedBy())
                .isNull();

        assertThat(application.getRejectionReason())
                .isNull();
    }

    @Test
    void shouldRejectCreationWhenFinancedAmountDoesNotMatchFinancialConditions() {

        /* Arrange */

        UUID applicationCode =
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                );

        BigDecimal inconsistentFinancedAmount =
                new BigDecimal("9999.99");

        /* Act */

        Throwable throwable = catchThrowable(() ->
                CreditApplication.create(
                        applicationCode,
                        "CA-00000001",
                        FinancingCurrency.PEN,
                        new BigDecimal("12000.00"),
                        new BigDecimal("2000.00"),
                        inconsistentFinancedAmount,
                        new BigDecimal("1.50"),
                        12,
                        1L,
                        2L,
                        1L
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_INVALID_FINANCED_AMOUNT
                );
    }

    @Test
    void shouldRejectCreationWhenDownPaymentIsNegative() {

        /* Arrange */

        BigDecimal negativeDownPayment =
                new BigDecimal("-100.00");

        /* Act */

        Throwable throwable = catchThrowable(() ->
                CreditApplication.create(
                        UUID.fromString(
                                "550e8400-e29b-41d4-a716-446655440001"
                        ),
                        "CA-00000001",
                        FinancingCurrency.PEN,
                        new BigDecimal("5000.00"),
                        negativeDownPayment,
                        new BigDecimal("5100.00"),
                        new BigDecimal("1.80"),
                        12,
                        1L,
                        2L,
                        1L
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_INVALID_DOWN_PAYMENT
                );
    }

    @Test
    void shouldRejectCreationWhenDownPaymentEqualsVehiclePrice() {

        /* Arrange */

        BigDecimal vehiclePrice =
                new BigDecimal("5000.00");

        BigDecimal downPayment =
                new BigDecimal("5000.00");

        /* Act */

        Throwable throwable = catchThrowable(() ->
                CreditApplication.create(
                        UUID.fromString(
                                "550e8400-e29b-41d4-a716-446655440001"
                        ),
                        "CA-00000001",
                        FinancingCurrency.PEN,
                        vehiclePrice,
                        downPayment,
                        new BigDecimal("0.00"),
                        new BigDecimal("1.80"),
                        12,
                        1L,
                        2L,
                        1L
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_INVALID_DOWN_PAYMENT
                );
    }

    @Test
    void shouldRejectCreationWhenCompensatoryInterestRateIsNegative() {

        /* Arrange */

        BigDecimal negativeInterestRate =
                new BigDecimal("-0.01");

        /* Act */

        Throwable throwable = catchThrowable(() ->
                CreditApplication.create(
                        UUID.fromString(
                                "550e8400-e29b-41d4-a716-446655440001"
                        ),
                        "CA-00000001",
                        FinancingCurrency.PEN,
                        new BigDecimal("12000.00"),
                        new BigDecimal("2000.00"),
                        new BigDecimal("10000.00"),
                        negativeInterestRate,
                        12,
                        1L,
                        2L,
                        1L
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_INVALID_COMPENSATORY_INTEREST_RATE
                );
    }

    @Test
    void shouldCreateApplicationWhenInstallmentCountIsAtMinimum() {

        /* Arrange */

        Integer installmentCount = 1;

        /* Act */

        CreditApplication application =
                CreditApplication.create(
                        UUID.fromString(
                                "550e8400-e29b-41d4-a716-446655440001"
                        ),
                        "CA-00000001",
                        FinancingCurrency.PEN,
                        new BigDecimal("12000.00"),
                        new BigDecimal("2000.00"),
                        new BigDecimal("10000.00"),
                        new BigDecimal("0.00"),
                        installmentCount,
                        1L,
                        2L,
                        1L
                );

        /* Assert */

        assertThat(application.getInstallmentCount())
                .isEqualTo(1);
    }

    @Test
    void shouldRejectCreationWhenInstallmentCountIsBelowMinimum() {

        /* Arrange */

        Integer invalidInstallmentCount = 0;

        /* Act */

        Throwable throwable = catchThrowable(() ->
                CreditApplication.create(
                        UUID.fromString(
                                "550e8400-e29b-41d4-a716-446655440001"
                        ),
                        "CA-00000001",
                        FinancingCurrency.PEN,
                        new BigDecimal("12000.00"),
                        new BigDecimal("2000.00"),
                        new BigDecimal("10000.00"),
                        new BigDecimal("1.50"),
                        invalidInstallmentCount,
                        1L,
                        2L,
                        1L
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_INVALID_INSTALLMENT_COUNT
                );
    }

    @Test
    void shouldRejectCreationWhenInstallmentCountIsAboveMaximum() {

        /* Arrange */

        Integer invalidInstallmentCount = 13;

        /* Act */

        Throwable throwable = catchThrowable(() ->
                CreditApplication.create(
                        UUID.fromString(
                                "550e8400-e29b-41d4-a716-446655440001"
                        ),
                        "CA-00000001",
                        FinancingCurrency.PEN,
                        new BigDecimal("12000.00"),
                        new BigDecimal("2000.00"),
                        new BigDecimal("10000.00"),
                        new BigDecimal("1.50"),
                        invalidInstallmentCount,
                        1L,
                        2L,
                        1L
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_INVALID_INSTALLMENT_COUNT
                );
    }

    @ParameterizedTest
    @EnumSource(RequiredField.class)
    void shouldRejectCreationWhenRequiredFieldIsNull(
            RequiredField requiredField) {

        /* Arrange */

        /* Act */

        Throwable throwable = catchThrowable(() ->
                createCreditApplicationWithNullField(
                        requiredField
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(NullPointerException.class)
                .hasMessage(
                        expectedNullMessage(requiredField)
                );
    }

    private enum RequiredField {

        APPLICATION_CODE,
        APPLICATION_NUMBER,
        CURRENCY,
        VEHICLE_PRICE,
        DOWN_PAYMENT,
        FINANCED_AMOUNT,
        COMPENSATORY_INTEREST_RATE,
        INSTALLMENT_COUNT,
        CUSTOMER_ID,
        VEHICLE_ID,
        BRANCH_ID

    }

    private CreditApplication createCreditApplication() {

        return CreditApplication.create(
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440000"
                ),
                "SOL-000001",
                FinancingCurrency.PEN,
                new BigDecimal("12000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("1.50"),
                12,
                1L,
                1L,
                1L
        );
    }

    private OffsetDateTime createResolvedAt() {

        return OffsetDateTime.of(
                2026,
                8,
                22,
                10,
                30,
                0,
                0,
                ZoneOffset.ofHours(-5)
        );
    }

    private CreditApplication createCreditApplicationWithNullField(
            RequiredField requiredField) {

        UUID applicationCode =
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                );

        String applicationNumber =
                "CA-00000001";

        FinancingCurrency currency =
                FinancingCurrency.PEN;

        BigDecimal vehiclePrice =
                new BigDecimal("12000.00");

        BigDecimal downPayment =
                new BigDecimal("2000.00");

        BigDecimal financedAmount =
                new BigDecimal("10000.00");

        BigDecimal compensatoryInterestRate =
                new BigDecimal("1.50");

        Integer installmentCount = 12;

        Long customerId = 1L;
        Long vehicleId = 2L;
        Long branchId = 1L;

        switch (requiredField) {

            case APPLICATION_CODE -> applicationCode = null;

            case APPLICATION_NUMBER -> applicationNumber = null;

            case CURRENCY -> currency = null;

            case VEHICLE_PRICE -> vehiclePrice = null;

            case DOWN_PAYMENT -> downPayment = null;

            case FINANCED_AMOUNT -> financedAmount = null;

            case COMPENSATORY_INTEREST_RATE -> compensatoryInterestRate = null;

            case INSTALLMENT_COUNT -> installmentCount = null;

            case CUSTOMER_ID -> customerId = null;

            case VEHICLE_ID -> vehicleId = null;

            case BRANCH_ID -> branchId = null;
        }

        return CreditApplication.create(
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
    }

    private String expectedNullMessage(
            RequiredField requiredField) {

        return switch (requiredField) {

            case APPLICATION_CODE -> "applicationCode must not be null";

            case APPLICATION_NUMBER -> "applicationNumber must not be null";

            case CURRENCY -> "currency must not be null";

            case VEHICLE_PRICE -> "vehiclePrice must not be null";

            case DOWN_PAYMENT -> "downPayment must not be null";

            case FINANCED_AMOUNT -> "financedAmount must not be null";

            case COMPENSATORY_INTEREST_RATE -> "compensatoryInterestRate must not be null";

            case INSTALLMENT_COUNT -> "installmentCount must not be null";

            case CUSTOMER_ID -> "customerId must not be null";

            case VEHICLE_ID -> "vehicleId must not be null";

            case BRANCH_ID -> "branchId must not be null";
        };
    }

}