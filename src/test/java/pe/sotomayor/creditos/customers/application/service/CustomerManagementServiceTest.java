package pe.sotomayor.creditos.customers.application.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mapstruct.factory.Mappers;

import pe.sotomayor.creditos.customers.application.dto.request.CreateCustomerRequest;
import pe.sotomayor.creditos.customers.application.dto.request.UpdateCustomerRequest;
import pe.sotomayor.creditos.customers.application.mapper.CustomerMapper;

import pe.sotomayor.creditos.customers.domain.model.entity.Customer;
import pe.sotomayor.creditos.customers.domain.repository.CustomerRepository;
import pe.sotomayor.creditos.customers.domain.service.CustomerCodeGenerator;
import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;

import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerManagementServiceTest {

    static final UUID CODE = UUID.fromString("550e8400-e29b-41d4-a716-446655440041");

    @Mock
    CustomerRepository repository;

    @Mock
    CurrentBranchProvider branches;

    @Mock
    CustomerCodeGenerator codes;

    CustomerMapper mapper = Mappers.getMapper(CustomerMapper.class);

    CreateCustomerService create;
    UpdateCustomerService update;
    GetCustomerService get;
    Customer current;

    @BeforeEach
    void setUp() {
        create = new CreateCustomerService(repository, mapper, branches, codes);
        update = new UpdateCustomerService(repository, mapper, branches);
        get = new GetCustomerService(repository, mapper, branches);
        current = Customer.create(
                CODE,
                "Cliente",
                CustomerDocumentType.DNI,
                "12345678",
                "900000001",
                "cliente@example.test",
                "Comerciante",
                new BigDecimal("2400.00"),
                "Direccion",
                null,
                3L
        );
        current.setId(8L);
        when(branches.getCurrentBranchId()).thenReturn(3L);
    }

    CreateCustomerRequest createRequest() {
        return new CreateCustomerRequest(
                "  Cliente  ",
                CustomerDocumentType.DNI,
                " 12345678 ",
                "900000001",
                "cliente@example.test",
                "Comerciante",
                new BigDecimal("2400.00"),
                "Direccion",
                null
        );
    }

    UpdateCustomerRequest updateRequest(String revision) {
        return new UpdateCustomerRequest(
                "Cliente actualizado",
                "900000002",
                "otro@example.test",
                "Independiente",
                new BigDecimal("3000.00"),
                "Nueva direccion",
                "Referencia",
                revision
        );
    }

    @Test
    void createUsesGeneratedIdentitySessionBranchAndInitialState() {

        /* Arrange */

        when(codes.generate()).thenReturn(CODE);
        when(repository.saveAndFlush(any()))
                .thenAnswer(invocation -> {
            Customer saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });

        /* Act */

        var response = create.create(createRequest());

        /* Assert */

        var captor = ArgumentCaptor.forClass(Customer.class);
        verify(repository).saveAndFlush(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getCustomerCode()).isEqualTo(CODE);
        assertThat(saved.getBranchId()).isEqualTo(3L);
        assertThat(saved.getFullName()).isEqualTo("Cliente");
        assertThat(saved.getDocumentNumber()).isEqualTo("12345678");
        assertThat(saved.getActive()).isTrue();
        assertThat(response.revision()).hasSize(64);
        assertThat(response.id()).isEqualTo(9L);
    }

    @Test
    void duplicateCreateStopsBeforeGeneratingIdentityOrWriting() {

        /* Arrange */

        when(repository.existsDocumentIncludingDeleted("DNI", "12345678"))
                .thenReturn(true);

        /* Act */

        var failure = catchThrowable(() -> create.create(createRequest()));

        /* Assert */

        assertThat(failure).isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_ALREADY_EXISTS);
        verifyNoInteractions(codes);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updatePreservesIdentityAndBranchAndChangesRevision() {

        /* Arrange */

        when(repository.findByCustomerCodeAndBranchIdForUpdate(CODE, 3L))
                .thenReturn(Optional.of(current));
        when(repository.saveAndFlush(current)).thenReturn(current);
        String before = mapper.revision(current);

        /* Act */

        var response = update.update(CODE, updateRequest(before));

        /* Assert */

        assertThat(current.getFullName()).isEqualTo("Cliente actualizado");
        assertThat(current.getDocumentNumber()).isEqualTo("12345678");
        assertThat(current.getDocumentType()).isEqualTo(CustomerDocumentType.DNI);
        assertThat(current.getActive()).isTrue();
        assertThat(current.getCustomerCode()).isEqualTo(CODE);
        assertThat(current.getBranchId()).isEqualTo(3L);
        assertThat(current.getId()).isEqualTo(8L);
        assertThat(response.revision()).isNotEqualTo(before);
    }

    @Test
    void staleFormDoesNotChangeOrSaveRecord() {

        /* Arrange */

        when(repository.findByCustomerCodeAndBranchIdForUpdate(CODE, 3L))
                .thenReturn(Optional.of(current));
        String before = mapper.revision(current);

        /* Act */

        var failure = catchThrowable(() -> update.update(CODE, updateRequest("0".repeat(64))));

        /* Assert */

        assertThat(failure).isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);
        assertThat(mapper.revision(current)).isEqualTo(before);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateDoesNotFallBackToUnscopedLookup() {

        /* Arrange */
        var request = updateRequest("0".repeat(64));

        /* Act */

        var failure = catchThrowable(() -> update.update(CODE, request));

        /* Assert */

        assertThat(failure).isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND);
        verify(repository).findByCustomerCodeAndBranchIdForUpdate(CODE, 3L);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void getReadsOnlyFromCurrentBranch() {

        /* Arrange */

        when(repository.findByCustomerCodeAndBranchId(CODE, 3L))
                .thenReturn(Optional.of(current));

        /* Act */

        var response = get.get(CODE);

        /* Assert */

        assertThat(response.customerCode()).isEqualTo(CODE);
        assertThat(response.revision()).isEqualTo(mapper.revision(current));
        verify(repository).findByCustomerCodeAndBranchId(CODE, 3L);
    }

    @Test
    void getHidesMissingDeletedAndOtherBranchResources() {

        /* Arrange */

        var code = CODE;

        /* Act */

        var failure = catchThrowable(() -> get.get(code));

        /* Assert */

        assertThat(failure).isInstanceOf(ResourceNotFoundException.class);
        verify(repository).findByCustomerCodeAndBranchId(CODE, 3L);
    }

}

