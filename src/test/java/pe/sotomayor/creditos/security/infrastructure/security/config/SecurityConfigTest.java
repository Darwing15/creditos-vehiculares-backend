package pe.sotomayor.creditos.security.infrastructure.security.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void shouldConfigureExplicitOriginsWhenOriginListIsValid() {
        /* Arrange */
        String origins = " http://localhost:5173, https://creditos.test ";
        MockHttpServletRequest request = createRequest();

        /* Act */
        UrlBasedCorsConfigurationSource source = config.corsConfigurationSource(origins);
        CorsConfiguration cors = source.getCorsConfiguration(request);

        /* Assert */
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).containsExactly("http://localhost:5173", "https://creditos.test");
        assertThat(cors.getAllowedMethods()).containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(cors.getAllowedHeaders()).containsExactly("Authorization", "Content-Type", "Accept");
        assertThat(cors.getExposedHeaders()).containsExactly("WWW-Authenticate");
        assertThat(cors.getAllowCredentials()).isFalse();
        assertThat(cors.getMaxAge()).isEqualTo(3600L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ,  ", "*", "https://*.test", "null", "https://valid.test,*"})
    void shouldRejectConfigurationWhenOriginIsNotExplicit(String origins) {
        /* Arrange */
        String configuredOrigins = origins;

        /* Act */
        Throwable thrown = catchThrowable(() -> config.corsConfigurationSource(configuredOrigins));

        /* Assert */
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    private MockHttpServletRequest createRequest() {
        return new MockHttpServletRequest("OPTIONS", "/api/auth/login");
    }
}

