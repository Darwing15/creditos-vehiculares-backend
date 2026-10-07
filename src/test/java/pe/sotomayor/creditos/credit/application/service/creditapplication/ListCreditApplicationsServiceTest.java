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
import pe.sotomayor.creditos.shared.security.CurrentBranchProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCreditApplicationsServiceTest {

    private static final UUID FIRST_APPLICATION_CODE =
            UUID.fromString(
                    "550e8400-e29b-41d4-a716-446655440001"
            );

    private static final UUID SECOND_APPLICATION_CODE =
            UUID.fromString(
                    "550e8400-e29b-41d4-a716-446655440002"
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
    private ListCreditApplicationsService service;

    @Test
    void shouldListCreditApplicationsByStatus() {

        /* Arrange */

        when(currentBranchProvider.getCurrentBranchId())
                .thenReturn(3L);

        CreditApplication firstApplication =
                createApplication(
                        FIRST_APPLICATION_CODE,
                        "CA-00000001"
                );

        CreditApplication secondApplication =
                createApplication(
                        SECOND_APPLICATION_CODE,
                        "CA-00000002"
                );

        CreditApplicationResponse firstResponse =
                createResponse(
                        FIRST_APPLICATION_CODE,
                        "CA-00000001"
                );

        CreditApplicationResponse secondResponse =
                createResponse(
                        SECOND_APPLICATION_CODE,
                        "CA-00000002"
                );

        when(creditApplicationRepository
                .findAllByStatusAndBranchId(
                        CreditApplicationStatus.PENDING,
                        3L
                ))
                .thenReturn(
                        List.of(
                                firstApplication,
                                secondApplication
                        )
                );

        when(creditApplicationMapper
                .toResponse(firstApplication))
                .thenReturn(firstResponse);

        when(creditApplicationMapper
                .toResponse(secondApplication))
                .thenReturn(secondResponse);

        /* Act */

        List<CreditApplicationResponse> result =
                service.listByStatus(
                        CreditApplicationStatus.PENDING
                );

        /* Assert */

        assertThat(result)
                .containsExactly(
                        firstResponse,
                        secondResponse
                );

        verify(creditApplicationRepository)
                .findAllByStatusAndBranchId(
                        CreditApplicationStatus.PENDING,
                        3L
                );

        verify(creditApplicationMapper)
                .toResponse(firstApplication);

        verify(creditApplicationMapper)
                .toResponse(secondApplication);
    }

    @Test
    void shouldReturnEmptyListWhenNoCreditApplicationsMatchStatus() {

        /* Arrange */

        when(currentBranchProvider.getCurrentBranchId())
                .thenReturn(3L);

        when(creditApplicationRepository
                .findAllByStatusAndBranchId(
                        CreditApplicationStatus.PENDING,
                        3L
                ))
                .thenReturn(List.of());

        /* Act */

        List<CreditApplicationResponse> result =
                service.listByStatus(
                        CreditApplicationStatus.PENDING
                );

        /* Assert */

        assertThat(result)
                .isEmpty();

        verify(creditApplicationRepository)
                .findAllByStatusAndBranchId(
                        CreditApplicationStatus.PENDING,
                        3L
                );

        verifyNoInteractions(
                creditApplicationMapper
        );
    }

    private CreditApplication createApplication(
            UUID applicationCode,
            String applicationNumber) {

        return CreditApplication.create(
                applicationCode,
                applicationNumber,
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

    private CreditApplicationResponse createResponse(
            UUID applicationCode,
            String applicationNumber) {

        return new CreditApplicationResponse(
                applicationCode,
                applicationNumber,
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
