package pe.sotomayor.creditos.credit.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.test.web.servlet.MockMvc;

import pe.sotomayor.creditos.config.TestSecurityConfig;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.request.CreateCreditApplicationRequest;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.request.RejectCreditApplicationRequest;
import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.application.usecase.creditapplication.ApproveCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.CancelCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.CreateCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.GetCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.ListCreditApplicationsUseCase;
import pe.sotomayor.creditos.credit.application.usecase.creditapplication.RejectCreditApplicationUseCase;

import pe.sotomayor.creditos.credit.application.usecase.creditsimulation.SimulateCreditApplicationUseCase;
import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;
import pe.sotomayor.creditos.credit.domain.model.enumeration.FinancingCurrency;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CreditApplicationController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc
class CreditApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateCreditApplicationUseCase
            createCreditApplicationUseCase;

    @MockitoBean
    private ApproveCreditApplicationUseCase
            approveCreditApplicationUseCase;

    @MockitoBean
    private RejectCreditApplicationUseCase
            rejectCreditApplicationUseCase;

    @MockitoBean
    private CancelCreditApplicationUseCase
            cancelCreditApplicationUseCase;

    @MockitoBean
    private GetCreditApplicationUseCase
            getCreditApplicationUseCase;

    @MockitoBean
    private ListCreditApplicationsUseCase
            listCreditApplicationsUseCase;

    @MockitoBean
    private SimulateCreditApplicationUseCase
            simulateCreditApplicationUseCase;

    @Test
    void shouldCreateCreditApplication() throws Exception {

        /* Arrange */

        CreateCreditApplicationRequest request =
                createRequest();

        CreditApplicationResponse response =
                createResponse();

        when(createCreditApplicationUseCase.create(request))
                .thenReturn(response);

        /* Act */

        var result = mockMvc.perform(
                post("/api/credit-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request
                                )
                        )
        );

        /* Assert */

        result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationCode")
                        .value(
                                "550e8400-e29b-41d4-a716-446655440001"
                        ))
                .andExpect(jsonPath("$.applicationNumber")
                        .value("CA-00000001"))
                .andExpect(jsonPath("$.currency")
                        .value("PEN"))
                .andExpect(jsonPath("$.status")
                        .value("PENDING"));

        verify(createCreditApplicationUseCase)
                .create(request);
    }

    @Test
    void shouldReturnBadRequestWhenCurrencyIsMissing() throws Exception {

        /* Arrange */

        CreateCreditApplicationRequest request =
                createRequestWithoutCurrency();

        /* Act */

        var result = mockMvc.perform(
                post("/api/credit-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request
                                )
                        )
        );

        /* Assert */

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(
                createCreditApplicationUseCase
        );
    }

    @Test
    void shouldApproveCreditApplication() throws Exception {

        /* Arrange */

        UUID applicationCode =
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                );

        CreditApplicationResponse response =
                createApprovedResponse();

        when(approveCreditApplicationUseCase
                .approve(applicationCode))
                .thenReturn(response);

        /* Act */

        var result = mockMvc.perform(
                patch(
                        "/api/credit-applications/{applicationCode}/approve",
                        applicationCode
                )
        );

        /* Assert */

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationCode")
                        .value(applicationCode.toString()))
                .andExpect(jsonPath("$.status")
                        .value("APPROVED"))
                .andExpect(jsonPath("$.resolvedBy")
                        .value(7));

        verify(approveCreditApplicationUseCase)
                .approve(applicationCode);
    }

    @Test
    void shouldRejectCreditApplication() throws Exception {

        /* Arrange */

        UUID applicationCode =
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                );

        RejectCreditApplicationRequest request =
                createRejectRequest();

        CreditApplicationResponse response =
                createRejectedResponse();

        when(rejectCreditApplicationUseCase.reject(
                applicationCode,
                request
        )).thenReturn(response);

        /* Act */

        var result = mockMvc.perform(
                patch(
                        "/api/credit-applications/{applicationCode}/reject",
                        applicationCode
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request
                                )
                        )
        );

        /* Assert */

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationCode")
                        .value(applicationCode.toString()))
                .andExpect(jsonPath("$.status")
                        .value("REJECTED"))
                .andExpect(jsonPath("$.resolvedBy")
                        .value(7))
                .andExpect(jsonPath("$.rejectionReason")
                        .value("Insufficient payment capacity"));

        verify(rejectCreditApplicationUseCase)
                .reject(
                        applicationCode,
                        request
                );
    }

    @Test
    void shouldReturnBadRequestWhenRejectionReasonIsBlank() throws Exception {

        /* Arrange */

        UUID applicationCode =
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                );

        RejectCreditApplicationRequest request =
                createRejectRequestWithBlankReason();

        /* Act */

        var result = mockMvc.perform(
                patch(
                        "/api/credit-applications/{applicationCode}/reject",
                        applicationCode
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(
                                        request
                                )
                        )
        );

        /* Assert */

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(
                rejectCreditApplicationUseCase
        );
    }

    @Test
    void shouldCancelCreditApplication() throws Exception {

        /* Arrange */

        UUID applicationCode =
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                );

        CreditApplicationResponse response =
                createCancelledResponse();

        when(cancelCreditApplicationUseCase
                .cancel(applicationCode))
                .thenReturn(response);

        /* Act */

        var result = mockMvc.perform(
                patch(
                        "/api/credit-applications/{applicationCode}/cancel",
                        applicationCode
                )
        );

        /* Assert */

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationCode")
                        .value(applicationCode.toString()))
                .andExpect(jsonPath("$.status")
                        .value("CANCELLED"))
                .andExpect(jsonPath("$.resolvedBy")
                        .value(7));

        verify(cancelCreditApplicationUseCase)
                .cancel(applicationCode);
    }

    @Test
    void shouldGetCreditApplicationByApplicationCode() throws Exception {

        /* Arrange */

        UUID applicationCode =
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                );

        CreditApplicationResponse response =
                createPendingResponse();

        when(getCreditApplicationUseCase
                .getByApplicationCode(applicationCode))
                .thenReturn(response);

        /* Act */

        var result = mockMvc.perform(
                get(
                        "/api/credit-applications/{applicationCode}",
                        applicationCode
                )
        );

        /* Assert */

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationCode")
                        .value(applicationCode.toString()))
                .andExpect(jsonPath("$.applicationNumber")
                        .value("CA-00000001"))
                .andExpect(jsonPath("$.status")
                        .value("PENDING"));

        verify(getCreditApplicationUseCase)
                .getByApplicationCode(applicationCode);
    }

    @Test
    void shouldListCreditApplicationsByStatus() throws Exception {

        /* Arrange */

        CreditApplicationResponse firstResponse =
                createPendingResponse(
                        UUID.fromString(
                                "550e8400-e29b-41d4-a716-446655440001"
                        ),
                        "CA-00000001"
                );

        CreditApplicationResponse secondResponse =
                createPendingResponse(
                        UUID.fromString(
                                "550e8400-e29b-41d4-a716-446655440002"
                        ),
                        "CA-00000002"
                );

        when(listCreditApplicationsUseCase
                .listByStatus(
                        CreditApplicationStatus.PENDING
                ))
                .thenReturn(
                        List.of(
                                firstResponse,
                                secondResponse
                        )
                );

        /* Act */

        var result = mockMvc.perform(
                get("/api/credit-applications")
                        .param("status", "PENDING")
        );

        /* Assert */

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()")
                        .value(2))
                .andExpect(jsonPath("$[0].applicationNumber")
                        .value("CA-00000001"))
                .andExpect(jsonPath("$[0].status")
                        .value("PENDING"))
                .andExpect(jsonPath("$[1].applicationNumber")
                        .value("CA-00000002"))
                .andExpect(jsonPath("$[1].status")
                        .value("PENDING"));

        verify(listCreditApplicationsUseCase)
                .listByStatus(
                        CreditApplicationStatus.PENDING
                );
    }

    @Test
    void shouldReturnBadRequestWhenListStatusIsMissing() throws Exception {

        /* Arrange */

        /* Act */

        var result = mockMvc.perform(
                get("/api/credit-applications")
        );

        /* Assert */

        result
                .andExpect(status().isBadRequest());

        verifyNoInteractions(
                listCreditApplicationsUseCase
        );
    }

    @Test
    void shouldReturnBadRequestWhenListStatusIsInvalid() throws Exception {

        /* Arrange */

        /* Act */

        var result = mockMvc.perform(
                get("/api/credit-applications")
                        .param("status", "INVALID")
        );

        /* Assert */

        result
                .andExpect(status().isBadRequest());

        verifyNoInteractions(
                listCreditApplicationsUseCase
        );
    }

    private CreateCreditApplicationRequest createRequest() {

        return new CreateCreditApplicationRequest(
                1L,
                2L,
                FinancingCurrency.PEN,
                new BigDecimal("12000.00"),
                new BigDecimal("2000.00"),
                12
        );
    }

    private CreateCreditApplicationRequest
    createRequestWithoutCurrency() {

        return new CreateCreditApplicationRequest(
                1L,
                2L,
                null,
                new BigDecimal("12000.00"),
                new BigDecimal("2000.00"),
                12
        );
    }

    private CreditApplicationResponse createResponse() {

        return new CreditApplicationResponse(
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                ),
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

    private CreditApplicationResponse createApprovedResponse() {

        return new CreditApplicationResponse(
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                ),
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
                CreditApplicationStatus.APPROVED,
                OffsetDateTime.parse(
                        "2026-08-26T15:00:00Z"
                ),
                7L,
                null
        );
    }

    private RejectCreditApplicationRequest createRejectRequest() {

        return new RejectCreditApplicationRequest(
                "Insufficient payment capacity"
        );
    }

    private CreditApplicationResponse createRejectedResponse() {

        return new CreditApplicationResponse(
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                ),
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
                CreditApplicationStatus.REJECTED,
                OffsetDateTime.parse(
                        "2026-08-26T15:00:00Z"
                ),
                7L,
                "Insufficient payment capacity"
        );
    }

    private RejectCreditApplicationRequest
    createRejectRequestWithBlankReason() {

        return new RejectCreditApplicationRequest(
                "   "
        );
    }

    private CreditApplicationResponse createCancelledResponse() {

        return new CreditApplicationResponse(
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                ),
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
                CreditApplicationStatus.CANCELLED,
                OffsetDateTime.parse(
                        "2026-08-26T15:00:00Z"
                ),
                7L,
                null
        );
    }

    private CreditApplicationResponse createPendingResponse() {

        return new CreditApplicationResponse(
                UUID.fromString(
                        "550e8400-e29b-41d4-a716-446655440001"
                ),
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

    private CreditApplicationResponse createPendingResponse(
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