package pe.sotomayor.creditos.security.infrastructure.security.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlerTest {

    @Test
    void shouldWriteUnauthorizedWhenSecurityRejectsRequest() throws Exception {
        /* Arrange */
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        SecurityErrorHandler service = new SecurityErrorHandler(mapper);
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        /* Act */
        service.commence(request, response, new BadCredentialsException("private-detail"));
        JsonNode json = mapper.readTree(response.getContentAsString());

        /* Assert */
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(json.path("status").asInt()).isEqualTo(401);
        assertThat(json.path("errorCode").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(json.path("path").asText()).isEqualTo("/api/auth/me");
        assertThat(json.path("timestamp").isMissingNode()).isFalse();
        assertThat(json.path("message").asText()).isNotBlank().doesNotContain("private-detail");
    }

    @Test
    void shouldWriteForbiddenWhenSecurityRejectsRequest() throws Exception {
        /* Arrange */
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        SecurityErrorHandler service = new SecurityErrorHandler(mapper);
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        /* Act */
        service.handle(request, response, new AccessDeniedException("private-detail"));
        JsonNode json = mapper.readTree(response.getContentAsString());

        /* Assert */
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("WWW-Authenticate")).isNull();
        assertThat(json.path("status").asInt()).isEqualTo(403);
        assertThat(json.path("errorCode").asText()).isEqualTo("FORBIDDEN");
        assertThat(json.path("path").asText()).isEqualTo("/api/auth/me");
        assertThat(json.path("timestamp").isMissingNode()).isFalse();
        assertThat(json.path("message").asText()).isNotBlank().doesNotContain("private-detail");
    }

    @Test
    void shouldWriteUnavailableWhenSecurityRejectsRequest() throws Exception {
        /* Arrange */
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        SecurityErrorHandler service = new SecurityErrorHandler(mapper);
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        /* Act */
        service.unavailable(request, response);
        JsonNode json = mapper.readTree(response.getContentAsString());

        /* Assert */
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("WWW-Authenticate")).isNull();
        assertThat(json.path("status").asInt()).isEqualTo(503);
        assertThat(json.path("errorCode").asText()).isEqualTo("SERVICE_UNAVAILABLE");
        assertThat(json.path("path").asText()).isEqualTo("/api/auth/me");
        assertThat(json.path("timestamp").isMissingNode()).isFalse();
        assertThat(json.path("message").asText()).isNotBlank().doesNotContain("private-detail");
    }

    private MockHttpServletRequest createRequest() {
        return new MockHttpServletRequest("GET", "/api/auth/me");
    }
}

