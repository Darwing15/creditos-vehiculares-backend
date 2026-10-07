package pe.sotomayor.creditos.shared.exception;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.mock.web.MockHttpServletRequest;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.security.authentication.BadCredentialsException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.sotomayor.creditos.shared.response.ApiError;
import pe.sotomayor.creditos.shared.error.ErrorCode;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void shouldReturnBadRequestWhenRequiredRequestParameterIsMissing()
            throws Exception {

        /* Arrange */

        /* Act */

        var result = mockMvc.perform(
                get("/test/missing-parameter")
        );

        /* Assert */

        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.errorCode")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "El parámetro requerido 'value' no está presente."
                        ))
                .andExpect(jsonPath("$.path")
                        .value(
                                "/test/missing-parameter"
                        ))
                .andExpect(jsonPath("$.timestamp")
                        .exists());
    }

    @Test
    void shouldReturnBadRequestWhenRequestParameterHasInvalidType()
            throws Exception {

        /* Arrange */

        /* Act */

        var result = mockMvc.perform(
                get("/test/type-mismatch")
                        .param("status", "INVALID")
        );

        /* Assert */

        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.errorCode")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "El parámetro 'status' contiene un valor inválido."
                        ))
                .andExpect(jsonPath("$.path")
                        .value(
                                "/test/type-mismatch"
                        ))
                .andExpect(jsonPath("$.timestamp")
                        .exists());
    }

    @Test
    void shouldReturnBadRequestWhenRequestBodyContainsMalformedJson()
            throws Exception {

        /* Arrange */

        String malformedJson =
                """
                        {
                          "name": "Darwin"
                        """;

        /* Act */

        var result = mockMvc.perform(
                post("/test/malformed-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson)
        );

        /* Assert */

        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.errorCode")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "El cuerpo de la solicitud contiene un formato inválido."
                        ))
                .andExpect(jsonPath("$.path")
                        .value(
                                "/test/malformed-json"
                        ))
                .andExpect(jsonPath("$.timestamp")
                        .exists());
    }

    @Test
    void shouldReturnBadRequestWhenRequestBodyFailsBeanValidation()
            throws Exception {

        /* Arrange */

        String invalidJson =
                """
                        {
                          "name": ""
                        }
                        """;

        /* Act */

        var result = mockMvc.perform(
                post("/test/bean-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)
        );

        /* Assert */

        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.errorCode")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "name: El nombre es obligatorio."
                        ))
                .andExpect(jsonPath("$.path")
                        .value(
                                "/test/bean-validation"
                        ))
                .andExpect(jsonPath("$.timestamp")
                        .exists());
    }

    @Test
    void shouldReturnBadRequestWhenConstraintViolationOccurs() {

        /* Arrange */

        try (var validatorFactory =
                     Validation.buildDefaultValidatorFactory()) {

            Validator validator =
                    validatorFactory.getValidator();

            ConstraintViolationRequest invalidRequest =
                    new ConstraintViolationRequest("");

            Set<ConstraintViolation<ConstraintViolationRequest>>
                    violations =
                    validator.validate(invalidRequest);

            ConstraintViolationException exception =
                    new ConstraintViolationException(
                            violations
                    );

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            request.setRequestURI(
                    "/test/constraint-violation"
            );

            GlobalExceptionHandler handler =
                    new GlobalExceptionHandler();

            /* Act */

            ResponseEntity<ApiError> result =
                    handler.handleConstraintViolation(
                            exception,
                            request
                    );

            /* Assert */

            assertThat(result.getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            assertThat(result.getBody())
                    .isNotNull()
                    .satisfies(apiError -> {

                        assertThat(apiError.getStatus())
                                .isEqualTo(400);

                        assertThat(apiError.getError())
                                .isEqualTo("Bad Request");

                        assertThat(apiError.getErrorCode())
                                .isEqualTo("VALIDATION_ERROR");

                        assertThat(apiError.getMessage())
                                .isEqualTo(
                                        "El valor es obligatorio."
                                );

                        assertThat(apiError.getPath())
                                .isEqualTo(
                                        "/test/constraint-violation"
                                );

                        assertThat(apiError.getTimestamp())
                                .isNotNull();
                    });
        }
    }

    @Test
    void shouldReturnBusinessExceptionStatusAndErrorCode()
            throws Exception {

        /* Arrange */

        /* Act */

        var result = mockMvc.perform(
                get("/test/business-exception")
        );

        /* Assert */

        result
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.errorCode")
                        .value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "El recurso solicitado no existe."
                        ))
                .andExpect(jsonPath("$.path")
                        .value(
                                "/test/business-exception"
                        ))
                .andExpect(jsonPath("$.timestamp")
                        .exists());
    }

    @Test
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid()
            throws Exception {

        /* Arrange */

        /* Act */

        var result = mockMvc.perform(
                get("/test/bad-credentials")
        );

        /* Assert */

        result
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status")
                        .value(401))
                .andExpect(jsonPath("$.error")
                        .value("Unauthorized"))
                .andExpect(jsonPath("$.errorCode")
                        .value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid credentials."))
                .andExpect(jsonPath("$.path")
                        .value("/test/bad-credentials"))
                .andExpect(jsonPath("$.timestamp")
                        .exists());
    }

    @Test
    void shouldReturnNotFoundWhenResourceDoesNotExist() {

        /* Arrange */

        NoResourceFoundException exception =
                new NoResourceFoundException(
                        HttpMethod.GET,
                        "/unknown-resource"
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setRequestURI(
                "/unknown-resource"
        );

        GlobalExceptionHandler handler =
                new GlobalExceptionHandler();

        /* Act */

        ResponseEntity<ApiError> result =
                handler.handleNoResourceFound(
                        exception,
                        request
                );

        /* Assert */

        assertThat(result.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(result.getBody())
                .isNotNull()
                .satisfies(apiError -> {

                    assertThat(apiError.getStatus())
                            .isEqualTo(404);

                    assertThat(apiError.getError())
                            .isEqualTo("Not Found");

                    assertThat(apiError.getErrorCode())
                            .isEqualTo("RESOURCE_NOT_FOUND");

                    assertThat(apiError.getMessage())
                            .isEqualTo(
                                    "El recurso solicitado no existe."
                            );

                    assertThat(apiError.getPath())
                            .isEqualTo(
                                    "/unknown-resource"
                            );

                    assertThat(apiError.getTimestamp())
                            .isNotNull();
                });
    }

    @Test
    void shouldReturnInternalServerErrorWhenUnexpectedExceptionOccurs()
            throws Exception {

        /* Arrange */

        /* Act */

        var result = mockMvc.perform(
                get("/test/unexpected-exception")
        );

        /* Assert */

        result
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status")
                        .value(500))
                .andExpect(jsonPath("$.error")
                        .value("Internal Server Error"))
                .andExpect(jsonPath("$.errorCode")
                        .value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Ha ocurrido un error interno. Intente nuevamente más tarde."
                        ))
                .andExpect(jsonPath("$.path")
                        .value(
                                "/test/unexpected-exception"
                        ))
                .andExpect(jsonPath("$.timestamp")
                        .exists());
    }

    @RestController
    private static class TestController {

        @GetMapping("/test/missing-parameter")
        String missingParameter(
                @RequestParam String value) {

            return value;
        }

        @GetMapping("/test/type-mismatch")
        String typeMismatch(
                @RequestParam TestStatus status) {

            return status.name();
        }

        @PostMapping("/test/malformed-json")
        String malformedJson(
                @RequestBody TestRequest request) {

            return request.name();
        }

        @PostMapping("/test/bean-validation")
        String beanValidation(
                @Valid
                @RequestBody ValidatedRequest request) {

            return request.name();
        }

        @GetMapping("/test/business-exception")
        String businessException() {

            throw new ResourceNotFoundException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "El recurso solicitado no existe."
            );
        }

        @GetMapping("/test/bad-credentials")
        String badCredentials() {

            throw new BadCredentialsException(
                    "Internal authentication failure."
            );
        }

        @GetMapping("/test/unexpected-exception")
        String unexpectedException() {

            throw new IllegalStateException(
                    "Sensitive technical details."
            );
        }
    }

    private enum TestStatus {
        VALID
    }

    private record TestRequest(
            String name
    ) {
    }

    private record ValidatedRequest(

            @NotBlank(
                    message = "El nombre es obligatorio."
            )
            String name

    ) {
    }

    private record ConstraintViolationRequest(

            @NotBlank(
                    message = "El valor es obligatorio."
            )
            String value

    ) {
    }

}
