package pe.sotomayor.creditos.credit.application.service.creditapplication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.application.mapper.creditapplication.CreditApplicationMapper;

import pe.sotomayor.creditos.credit.domain.model.entity.CreditApplication;

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;
import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import pe.sotomayor.creditos.credit.domain.repository.CreditApplicationRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCreditApplicationServiceTest {

    private static final UUID APPLICATION_CODE =
            UUID.fromString(
                    "550e8400-e29b-41d4-a716-446655440001"
            );

    @Mock
    private CurrentBranchProvider
            currentBranchProvider;

    @Mock
    private CreditApplicationRepository
            creditApplicationRepository;

    @Mock
    private CreditApplicationMapper
            creditApplicationMapper;

    @InjectMocks
    private GetCreditApplicationService service;

    @Test
    void shouldGetCreditApplicationByApplicationCode() {

        /* Arrange */

        when(currentBranchProvider.getCurrentBranchId())
                .thenReturn(3L);

        CreditApplication application =
                createApplication();

        CreditApplicationResponse response =
                createResponse();

        when(creditApplicationRepository
                .findByApplicationCodeAndBranchId(
                        APPLICATION_CODE,
                        3L))
                .thenReturn(Optional.of(application));

        when(creditApplicationMapper.toResponse(application))
                .thenReturn(response);

        /* Act */

        CreditApplicationResponse result =
                service.getByApplicationCode(
                        APPLICATION_CODE
                );

        /* Assert */

        assertThat(result)
                .isSameAs(response);

        verify(creditApplicationRepository)
                .findByApplicationCodeAndBranchId(
                        APPLICATION_CODE,
                        3L);

        verify(creditApplicationMapper)
                .toResponse(application);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCreditApplicationDoesNotExist() {

        /* Arrange */

        when(currentBranchProvider.getCurrentBranchId())
                .thenReturn(3L);

        when(creditApplicationRepository
                .findByApplicationCodeAndBranchId(
                        APPLICATION_CODE,
                        3L))
                .thenReturn(Optional.empty());

        /* Act */

        Throwable throwable = catchThrowable(
                () -> service.getByApplicationCode(
                        APPLICATION_CODE
                )
        );

        /* Assert */

        assertThat(throwable)
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.CREDIT_APPLICATION_NOT_FOUND
                );

        verifyNoInteractions(
                creditApplicationMapper
        );
    }

    private CreditApplication createApplication() {

        return CreditApplication.create(
                APPLICATION_CODE,
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
    }

    private CreditApplicationResponse createResponse() {

        return new CreditApplicationResponse(
                APPLICATION_CODE,
                "CA-00000001",
                1L,
                2L,
                3L,
                FinancingCurrency.PEN,
                new BigDecimal("12000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("1.50"),
                12,
                CreditApplicationStatus.PENDING,
                null,
                null,
                null
        );
    }

}
