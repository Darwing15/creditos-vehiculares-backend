package pe.sotomayor.creditos.customers.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.customers.application.dto.request.CreateCustomerRequest;
import pe.sotomayor.creditos.customers.application.mapper.CustomerMapper;

import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;
import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;
import pe.sotomayor.creditos.customers.domain.service.CustomerCodeGenerator;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.shared.exception.ValidationException;
import pe.sotomayor.creditos.shared.error.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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

