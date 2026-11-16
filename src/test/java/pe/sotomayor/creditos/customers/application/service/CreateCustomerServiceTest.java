package pe.sotomayor.creditos.customers.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.customers.application.dto.request.CreateCustomerRequest;
import pe.sotomayor.creditos.customers.application.mapper.CustomerMapper;
import pe.sotomayor.creditos.customers.domain.model.entity.Customer;
import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;
import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;
import pe.sotomayor.creditos.customers.domain.service.CustomerCodeGenerator;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.shared.exception.ValidationException;
import pe.sotomayor.creditos.shared.error.ErrorCode;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateCustomerServiceTest {

    @Mock
    CustomerRepository repository;

    @Mock
    CustomerMapper mapper;

    @Mock
    CurrentBranchProvider branches;

    @Mock
    CustomerCodeGenerator codeGenerator;

    @InjectMocks
    CreateCustomerService service;

    @Test
    void shouldRejectCreationWhenDniIsIncomplete() {

        /* Arrange */

        var valid = createRequest();
        var request = new CreateCustomerRequest(
                valid.fullName(),
                valid.documentType(),
                "1234567",
                null,
                null,
                null,
                null,
                null,
                null
        );

        /* Act */

        var failure = catchThrowable(() -> service.create(request));

        /* Assert */

        assertThat(failure)
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDENTITY_DOCUMENT_INVALID);

        verifyNoInteractions(repository, mapper, branches, codeGenerator);
        verifyNoMoreInteractions(repository, mapper, branches, codeGenerator);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "00001234", "ab00123", "ABC123456789012"})
    void shouldSaveValidCeInUppercasePreservingZeros(String number) {

        /* Arrange */

        var request = new CreateCustomerRequest(
                "Cliente",
                CustomerDocumentType.CE,
                number,
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(branches.getCurrentBranchId()).thenReturn(3L);
        when(codeGenerator.generate()).thenReturn(UUID.randomUUID());
        when(repository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        /* Act */

        service.create(request);

        var customer = ArgumentCaptor.forClass(Customer.class);

        /* Assert */

        verify(repository).saveAndFlush(customer.capture());
        assertThat(customer.getValue().getDocumentNumber())
                .isEqualTo(number.toUpperCase(Locale.ROOT));
        verify(repository).existsDocumentIncludingDeleted("CE", number.toUpperCase(Locale.ROOT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "1234567890123456", "AB-123", "AB 123", "ABÑ123", "ABß123"})
    void shouldRejectInvalidCeWithoutSaving(String number) {

        /* Arrange */

        var request = new CreateCustomerRequest("Cliente", CustomerDocumentType.CE,
                number, null, null, null, null, null, null);

        /* Act */

        var failure = catchThrowable(() -> service.create(request));

        /* Assert */

        assertThat(failure).isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_INVALID_CE);
        verifyNoInteractions(repository, mapper, branches, codeGenerator);
    }

    private CreateCustomerRequest createRequest() {
        return new CreateCustomerRequest(
                "Cliente",
                CustomerDocumentType.DNI,
                "00000001",
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}

