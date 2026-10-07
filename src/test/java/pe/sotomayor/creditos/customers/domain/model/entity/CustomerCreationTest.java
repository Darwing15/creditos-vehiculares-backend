package pe.sotomayor.creditos.customers.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerCreationTest {

    @Test
    void shouldCreateActiveCustomerWithRegistrationDataAndNoPersistenceIdentity() {

        /* Arrange */

        UUID code = createCode();

        /* Act */

        Customer customer = Customer.create(
                code,
                "Cliente de prueba",
                CustomerDocumentType.DNI,
                "12345678",
                "999123456",
                "cliente@example.com",
                "Comerciante",
                new BigDecimal("3500.00"),
                "Direccion de prueba",
                "Referencia de prueba",
                3L
        );

        /* Assert */

        assertThat(customer.getCustomerCode()).isEqualTo(code);
        assertThat(customer.getFullName()).isEqualTo("Cliente de prueba");
        assertThat(customer.getDocumentType()).isEqualTo(CustomerDocumentType.DNI);
        assertThat(customer.getDocumentNumber()).isEqualTo("12345678");
        assertThat(customer.getPhone()).isEqualTo("999123456");
        assertThat(customer.getEmail()).isEqualTo("cliente@example.com");
        assertThat(customer.getOccupation()).isEqualTo("Comerciante");
        assertThat(customer.getMonthlyIncome()).isEqualByComparingTo("3500.00");
        assertThat(customer.getAddress()).isEqualTo("Direccion de prueba");
        assertThat(customer.getMapReference()).isEqualTo("Referencia de prueba");
        assertThat(customer.getBranchId()).isEqualTo(3L);
        assertThat(customer.getActive()).isTrue();
        assertThat(customer.getDeleted()).isFalse();
        assertThat(customer.getId()).isNull();
        assertThat(customer.getCreatedBy()).isNull();
        assertThat(customer.getCreatedAt()).isNull();
    }

    @Test
    void shouldAllowOptionalDataToBeAbsentWithoutInventingIncome() {

        /* Arrange */

        UUID code = createCode();

        /* Act */

        Customer customer = createCustomer(
                code,
                "Cliente de prueba",
                CustomerDocumentType.CE,
                "CE-12345",
                null,
                null,
                null,
                null,
                null,
                null,
                3L
        );

        /* Assert */

        assertThat(customer.getPhone()).isNull();
        assertThat(customer.getEmail()).isNull();
        assertThat(customer.getOccupation()).isNull();
        assertThat(customer.getMonthlyIncome()).isNull();
        assertThat(customer.getAddress()).isNull();
        assertThat(customer.getMapReference()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectMissingName(String fullName) {

        /* Arrange */

        UUID code = createCode();

        /* Act / Assert */

        assertThatThrownBy(() -> createCustomer(
                code,
                fullName,
                CustomerDocumentType.DNI,
                "12345678",
                null,
                null,
                null,
                null,
                null,
                null,
                3L)
        ).isInstanceOf(ValidationException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void shouldRejectNameThatExceedsSchemaLength() {

        /* Arrange */

        String fullName = "A".repeat(151);

        /* Act / Assert */

        assertThatThrownBy(() -> createCustomer(
                createCode(),
                fullName,
                CustomerDocumentType.DNI,
                "12345678",
                null,
                null,
                null,
                null,
                null,
                null,
                3L)
        ).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "123456789012345678901"})
    void shouldRejectInvalidDocumentNumber(String documentNumber) {

        /* Arrange */

        UUID code = createCode();

        /* Act / Assert */

        assertThatThrownBy(() -> createCustomer(
                code,
                "Cliente",
                CustomerDocumentType.DNI,
                documentNumber,
                null,
                null,
                null,
                null,
                null,
                null,
                3L)
        ).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "10000000000.00", "10.001"})
    void shouldRejectIncomeOutsideSchemaConstraints(String amount) {

        /* Arrange */

        BigDecimal income = new BigDecimal(amount);

        /* Act / Assert */

        assertThatThrownBy(() -> createCustomer(
                createCode(),
                "Cliente",
                CustomerDocumentType.DNI,
                "12345678",
                null,
                null,
                null,
                income,
                null,
                null,
                3L)
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldAllowZeroIncomeAsSpecifiedBySchema() {

        /* Arrange */

        BigDecimal income = new BigDecimal("0.00");

        /* Act */

        Customer customer = createCustomer(
                createCode(),
                "Cliente",
                CustomerDocumentType.DNI,
                "12345678",
                null,
                null,
                null,
                income,
                null,
                null,
                3L
        );

        /* Assert */

        assertThat(customer.getMonthlyIncome()).isEqualByComparingTo(income);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "code",
            "documentType",
            "branch",
            "phone",
            "email",
            "occupation",
            "address",
            "mapReference"
    })
    void shouldRejectInvalidRegistrationField(String field) {

        /* Arrange */

        UUID code = field.equals("code") ? null : createCode();
        CustomerDocumentType documentType = field.equals("documentType") ? null : CustomerDocumentType.DNI;
        Long branch = field.equals("branch") ? null : 3L;
        String phone = field.equals("phone") ? "1".repeat(21) : null;
        String email = field.equals("email") ? "a".repeat(121) : null;
        String occupation = field.equals("occupation") ? "a".repeat(151) : null;
        String address = field.equals("address") ? "a".repeat(256) : null;
        String reference = field.equals("mapReference") ? "a".repeat(256) : null;

        /* Act / Assert */

        assertThatThrownBy(() -> createCustomer(
                code,
                "Cliente",
                documentType,
                "12345678",
                phone,
                email,
                occupation,
                null,
                address,
                reference,
                branch)
        ).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void shouldRejectNonPositiveBranchId(long branchId) {

        /* Arrange */

        UUID code = createCode();

        /* Act / Assert */

        assertThatThrownBy(() -> createCustomer(
                code,
                "Cliente",
                CustomerDocumentType.DNI,
                "12345678",
                null,
                null,
                null,
                null,
                null,
                null,
                branchId)
        ).isInstanceOf(ValidationException.class);
    }

    private UUID createCode() {
        return UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    }

    private Customer createCustomer(
            UUID code,
            String fullName,
            CustomerDocumentType documentType,
            String documentNumber,
            String phone,
            String email,
            String occupation,
            BigDecimal income,
            String address,
            String reference,
            Long branchId) {

        return Customer.create(
                code,
                fullName,
                documentType,
                documentNumber,
                phone,
                email,
                occupation,
                income,
                address,
                reference,
                branchId
        );
    }
}

