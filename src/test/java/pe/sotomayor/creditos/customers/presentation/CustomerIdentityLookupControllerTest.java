package pe.sotomayor.creditos.customers.presentation;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import pe.sotomayor.creditos.customers.application.usecase.LookupCustomerIdentityUseCase;
import pe.sotomayor.creditos.customers.application.dto.request.IdentityLookupRequest;
import pe.sotomayor.creditos.customers.application.dto.response.IdentityLookupResponse;

import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;
import pe.sotomayor.creditos.customers.domain.exception.IdentityLookupException;

import pe.sotomayor.creditos.security.infrastructure.security.config.SecurityConfig;
import pe.sotomayor.creditos.security.infrastructure.security.config.SecurityErrorHandler;
import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(controllers = CustomerIdentityLookupController.class,
        properties = "app.security.allowed-origins=http://localhost:5173")
@Import({SecurityConfig.class, SecurityErrorHandler.class})
class CustomerIdentityLookupControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    LookupCustomerIdentityUseCase lookup;

    @MockitoBean
    JwtService jwt;

    @MockitoBean
    CustomUserDetailsService users;

    @Test
    @WithMockUser(authorities = "customers:write")
    void shouldReturnMinimalContractWhenOperatorIsAuthorized() throws Exception {

        /* Arrange */

        var request = createRequest();
        when(lookup.lookup(request)).thenReturn(createResponse());

        /* Act */

        var response = mvc.perform(post("/api/customers/identity-lookups")
                        .contentType("application/json").content(createBody()))
                .andReturn().getResponse();

        /* Assert */

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getContentAsString())
                .contains("\"documentNumber\":\"00000001\"", "\"fullName\":\"NOMBRES UNO DOS\"")
                .doesNotContain("token", "direccion", "branchId");
        verify(lookup).lookup(request);
        verifyNoMoreInteractions(lookup);
    }

    @Test
    void shouldRejectRequestWhenSessionIsMissing() throws Exception {

        /* Arrange */

        var body = createBody();

        /* Act */

        var response = mvc.perform(post("/api/customers/identity-lookups")
                .contentType("application/json")
                .content(body)).andReturn().getResponse();

        /* Assert */

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(lookup);
        verifyNoMoreInteractions(lookup);
    }

    @Test
    @WithMockUser(authorities = "customers:read")
    void shouldRejectRequestWhenOperatorHasOnlyReadPermission() throws Exception {

        /* Arrange */

        var body = createBody();

        /* Act */

        var response = mvc.perform(post("/api/customers/identity-lookups")
                .contentType("application/json")
                .content(body)).andReturn().getResponse();

        /* Assert */
        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(lookup);
        verifyNoMoreInteractions(lookup);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1234567", "123456789", "abcdefgh", "１２３４５６７８"})
    @WithMockUser(authorities = "customers:write")
    void shouldRejectRequestWhenDniFormatIsInvalid(String number) throws Exception {

        /* Arrange */

        var body = "{\"documentType\":\"DNI\",\"documentNumber\":\"" + number + "\"}";

        /* Act */

        var response = mvc.perform(post("/api/customers/identity-lookups")
                .contentType("application/json")
                .content(body)).andReturn().getResponse();

        /* Assert */

        assertThat(response.getStatus()).isEqualTo(400);
        verifyNoInteractions(lookup);
        verifyNoMoreInteractions(lookup);
    }

    @Test
    @WithMockUser(authorities = "customers:write")
    void shouldPreserveSessionStatusWhenExternalProviderIsUnavailable() throws Exception {

        /* Arrange */

        var request = createRequest();
        when(lookup.lookup(request))
                .thenThrow(IdentityLookupException.unavailable());

        /* Act */

        var response = mvc.perform(post("/api/customers/identity-lookups")
                .contentType("application/json")
                .content(createBody())).andReturn().getResponse();

        /* Assert */

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("IDENTITY_PROVIDER_UNAVAILABLE");
        verify(lookup).lookup(request);
        verifyNoMoreInteractions(lookup);
    }

    private String createBody() {
        return "{\"documentType\":\"DNI\",\"documentNumber\":\"00000001\"}";
    }

    private IdentityLookupRequest createRequest() {
        return new IdentityLookupRequest(CustomerDocumentType.DNI, "00000001");
    }

    private IdentityLookupResponse createResponse() {
        return new IdentityLookupResponse(
                CustomerDocumentType.DNI,
                "00000001",
                "NOMBRES",
                "UNO",
                "DOS",
                "NOMBRES UNO DOS",
                "FACTILIZA",
                false,
                Instant.parse("2026-10-11T15:00:00Z"),
                Instant.parse("2026-11-10T15:00:00Z")
        );
    }
}

