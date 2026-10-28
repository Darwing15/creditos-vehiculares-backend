package pe.sotomayor.creditos.customers.infrastructure.identity;

import java.net.SocketTimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import pe.sotomayor.creditos.customers.infrastructure.config.IdentityLookupProperties;
import pe.sotomayor.creditos.customers.domain.exception.IdentityLookupException;
import pe.sotomayor.creditos.shared.error.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FactilizaIdentityLookupProviderTest {

    MockRestServiceServer server;
    FactilizaIdentityLookupProvider provider;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://api.factiliza.com/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new FactilizaIdentityLookupProvider(
                builder.build(), new ObjectMapper(), createProperties());
    }

    @Test
    void shouldReturnMinimalNamesWhenProviderReturnsValidData() {

        /* Arrange */

        server.expect(requestTo("https://api.factiliza.com/v1/dni/info/00000001"))
                .andExpect(header("Authorization", "Bearer synthetic-test-token"))
                .andRespond(withSuccess(createBody(), MediaType.APPLICATION_JSON));

        /* Act */

        var result = provider.lookup("00000001");

        /* Assert */

        assertThat(result.fullName()).isEqualTo("NOMBRES UNO DOS");
        assertThat(result.documentNumber()).isEqualTo("00000001");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 402, 403, 500, 503})
    void shouldHideExternalFailureWhenProviderRejectsRequest(int status) {

        /* Arrange */

        server.expect(requestTo("https://api.factiliza.com/v1/dni/info/00000001"))
                .andRespond(withStatus(HttpStatus.valueOf(status))
                        .body("secret external details"));

        /* Act */

        var failure = catchThrowable(() -> provider.lookup("00000001"));

        /* Assert */

        assertThat(failure)
                .isInstanceOf(IdentityLookupException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE);
        assertThat(failure).hasNoCause().hasMessageNotContaining("secret");
        server.verify();
    }

    @Test
    void shouldRejectResultWhenDocumentDoesNotMatchRequest() {

        /* Arrange */

        server.expect(requestTo("https://api.factiliza.com/v1/dni/info/00000001"))
                .andRespond(withSuccess(createBody()
                                .replace("00000001", "00000002"),
                        MediaType.APPLICATION_JSON));

        /* Act */

        var failure = catchThrowable(() -> provider.lookup("00000001"));

        /* Assert */

        assertThat(failure)
                .isInstanceOf(IdentityLookupException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDENTITY_PROVIDER_INVALID_RESPONSE);
        server.verify();
    }

    @Test
    void shouldRejectResultWhenBodyIsNotJson() {

        /* Arrange */

        server.expect(requestTo("https://api.factiliza.com/v1/dni/info/00000001"))
                .andRespond(withSuccess("<html>unavailable</html>", MediaType.TEXT_HTML));

        /* Act */

        var failure = catchThrowable(() -> provider.lookup("00000001"));

        /* Assert */

        assertThat(failure)
                .isInstanceOf(IdentityLookupException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDENTITY_PROVIDER_INVALID_RESPONSE);
        server.verify();
    }

    @Test
    void shouldRejectResultWhenBodyExceedsLimit() {

        /* Arrange */

        server.expect(requestTo("https://api.factiliza.com/v1/dni/info/00000001"))
                .andRespond(withSuccess(" ".repeat(65_537), MediaType.APPLICATION_JSON));

        /* Act */

        var failure = catchThrowable(() -> provider.lookup("00000001"));

        /* Assert */

        assertThat(failure)
                .isInstanceOf(IdentityLookupException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDENTITY_PROVIDER_INVALID_RESPONSE);
        server.verify();
    }

    @Test
    void shouldReportTimeoutWhenTransportDeadlineExpires() {

        /* Arrange */

        server.expect(requestTo("https://api.factiliza.com/v1/dni/info/00000001"))
                .andRespond(withException(new SocketTimeoutException("must stay private")));

        /* Act */

        var failure = catchThrowable(() -> provider.lookup("00000001"));

        /* Assert */

        assertThat(failure)
                .isInstanceOf(IdentityLookupException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDENTITY_PROVIDER_TIMEOUT);
        assertThat(failure).hasNoCause();
        server.verify();
    }

    @Test
    void shouldReportNotFoundWhenProviderReturns404() {

        /* Arrange */

        server.expect(requestTo("https://api.factiliza.com/v1/dni/info/00000001"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        /* Act */

        var failure = catchThrowable(() -> provider.lookup("00000001"));

        /* Assert */

        assertThat(failure)
                .isInstanceOf(IdentityLookupException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDENTITY_NOT_FOUND);
        server.verify();
    }

    @Test
    void shouldReportRateLimitWhenProviderReturns429() {

        /* Arrange */

        server.expect(requestTo("https://api.factiliza.com/v1/dni/info/00000001"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        /* Act */

        var failure = catchThrowable(() -> provider.lookup("00000001"));

        /* Assert */

        assertThat(failure)
                .isInstanceOf(IdentityLookupException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDENTITY_RATE_LIMITED);
        server.verify();
    }

    private IdentityLookupProperties createProperties() {
        var properties = new IdentityLookupProperties();
        properties.setEnabled(true);
        properties.setToken("synthetic-test-token");
        return properties;
    }

    private String createBody() {
        return """
                {"status":200,"success":true,"data":{"numero":"00000001","nombres":"NOMBRES",
                 "apellido_paterno":"UNO","apellido_materno":"DOS","direccion":"IGNORAR"}}
                """;
    }
}

