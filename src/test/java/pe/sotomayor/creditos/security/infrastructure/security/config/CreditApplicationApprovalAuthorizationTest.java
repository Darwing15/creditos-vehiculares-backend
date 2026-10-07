package pe.sotomayor.creditos.security.infrastructure.security.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

import pe.sotomayor.creditos.credit.presentation.controller.CreditApplicationController;

import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CreditApplicationController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class})
class CreditApplicationApprovalAuthorizationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtService jwt;

    @MockitoBean
    private CustomUserDetailsService users;

    @MockitoBean
    private CreateCreditApplicationUseCase create;

    @MockitoBean
    private GetCreditApplicationUseCase getApplication;

    @MockitoBean
    private ListCreditApplicationsUseCase list;

    @MockitoBean
    private ApproveCreditApplicationUseCase approve;

    @MockitoBean
    private RejectCreditApplicationUseCase reject;

    @MockitoBean
    private CancelCreditApplicationUseCase cancel;

    @MockitoBean
    private SimulateCreditApplicationUseCase simulate;

    @Test
    void shouldRejectAnonymousApproval() throws Exception {
        /* Arrange */
        UUID code = createApplicationCode();
        /* Act */
        mvc.perform(patch("/api/credit-applications/{code}/approve", code))
                .andExpect(status().isUnauthorized());
        /* Assert */
        verifyNoInteractions(approve);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldNotAuthorizeApprovalFromRoleNameAlone() throws Exception {
        /* Arrange */
        UUID code = createApplicationCode();
        /* Act */
        mvc.perform(patch("/api/credit-applications/{code}/approve", code))
                .andExpect(status().isForbidden());
        /* Assert */
        verifyNoInteractions(approve);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "credit-applications:read",
            "credit-applications:create",
            "customers:read",
            "vehicles:read"
    })
    void shouldRejectApprovalWithoutResolveAuthority(String permission) throws Exception {
        /* Arrange */
        UUID code = createApplicationCode();
        /* Act */
        mvc.perform(patch("/api/credit-applications/{code}/approve", code)
                        .with(user("reviewer").authorities(new SimpleGrantedAuthority(permission))))
                .andExpect(status().isForbidden());
        /* Assert */
        verifyNoInteractions(approve);
    }

    @Test
    @WithMockUser(authorities = "credit-applications:resolve")
    void shouldApproveWithExistingResolveAuthorityAndNoRequestBody() throws Exception {
        /* Arrange */
        UUID code = createApplicationCode();
        when(approve.approve(code)).thenReturn(createResponse());
        /* Act */
        mvc.perform(patch("/api/credit-applications/{code}/approve", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationCode").value(code.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.resolvedBy").value(7));
        /* Assert */
        verify(approve).approve(code);
        verifyNoMoreInteractions(approve);
        verifyNoInteractions(create, getApplication, list, reject, cancel, simulate);
    }

    @Test
    @WithMockUser(authorities = "credit-applications:resolve")
    void shouldRequireReadAuthorityToReviewDetails() throws Exception {
        /* Arrange */
        UUID code = createApplicationCode();
        /* Act */
        mvc.perform(get("/api/credit-applications/{code}", code))
                .andExpect(status().isForbidden());
        /* Assert */
        verifyNoInteractions(getApplication);
    }

    @Test
    @WithMockUser(authorities = "credit-applications:read")
    void shouldAllowReadOnlyReview() throws Exception {
        /* Arrange */
        UUID code = createApplicationCode();
        when(getApplication.getByApplicationCode(code)).thenReturn(createResponse());
        /* Act */
        mvc.perform(get("/api/credit-applications/{code}", code))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status")
                        .value("APPROVED"));
        /* Assert */
        verify(getApplication).getByApplicationCode(code);
        verifyNoMoreInteractions(getApplication);
        verifyNoInteractions(approve);
    }

    private UUID createApplicationCode() {
        return UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    }

    private CreditApplicationResponse createResponse() {
        return new CreditApplicationResponse(
                createApplicationCode(),
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
                OffsetDateTime.parse("2026-10-01T15:00:00Z"),
                7L,
                null
        );
    }
}
