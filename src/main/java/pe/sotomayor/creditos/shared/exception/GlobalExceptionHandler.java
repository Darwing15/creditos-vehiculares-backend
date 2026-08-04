package pe.sotomayor.creditos.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.security.authentication.BadCredentialsException;

import org.springframework.validation.FieldError;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.servlet.resource.NoResourceFoundException;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.response.ApiError;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones de la aplicación.
 *
 * <p>
 * Centraliza el tratamiento de todas las excepciones producidas por la
 * aplicación, garantizando respuestas HTTP uniformes y consistentes para
 * todos los consumidores de la API.
 * </p>
 *
 * <p>
 * Las excepciones de negocio son transformadas utilizando la información
 * proporcionada por {@link BusinessException}, mientras que las excepciones
 * técnicas son adaptadas al contrato estándar {@link ApiError}.
 * </p>
 *
 * @author DarwinG
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja todas las excepciones de negocio.
     *
     * @param exception excepción de negocio
     * @param request   petición HTTP
     * @return respuesta estandarizada
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request) {

        return buildResponse(
                exception.getHttpStatus(),
                exception.getErrorCode(),
                exception.getMessage(),
                request
        );
    }

    /**
     * Maneja errores de validación sobre objetos anotados con Bean Validation.
     *
     * @param exception excepción de validación
     * @param request   petición HTTP
     * @return respuesta estandarizada
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                message,
                request
        );
    }

    /**
     * Maneja violaciones de restricciones sobre parámetros.
     *
     * @param exception excepción de validación
     * @param request   petición HTTP
     * @return respuesta estandarizada
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {

        String message = exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                message,
                request
        );
    }

    /**
     * Maneja solicitudes con JSON inválido o mal formado.
     *
     * @param exception excepción producida por Spring
     * @param request   petición HTTP
     * @return respuesta estandarizada
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                "El cuerpo de la solicitud contiene un formato inválido.",
                request
        );
    }

    /**
     * Maneja credenciales de autenticación inválidas.
     *
     * <p>
     * Este método captura la excepción lanzada por Spring Security cuando
     * el correo electrónico o la contraseña proporcionados no corresponden
     * a un usuario válido del sistema.
     * </p>
     *
     * <p>
     * La respuesta mantiene el contrato estándar de errores de la aplicación,
     * evitando exponer información sensible sobre el proceso interno de
     * autenticación.
     * </p>
     *
     * @param exception excepción producida por Spring Security
     * @param request   solicitud HTTP
     * @return respuesta estandarizada con código HTTP 401
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.INVALID_CREDENTIALS,
                "Invalid credentials.",
                request
        );
    }

    /**
     * Maneja solicitudes hacia recursos estáticos inexistentes.
     *
     * <p>
     * Este método captura las peticiones realizadas a recursos que no
     * existen dentro de la aplicación (por ejemplo, una URL incorrecta
     * para Swagger UI) y responde con un código HTTP 404.
     * </p>
     *
     * @param ex      excepción producida por Spring MVC
     * @param request solicitud HTTP
     * @return respuesta de recurso no encontrado
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        ApiError response = ApiError.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .errorCode(ErrorCode.RESOURCE_NOT_FOUND.name())
                .message("El recurso solicitado no existe.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Maneja cualquier excepción no controlada por la aplicación.
     *
     * @param exception excepción inesperada
     * @param request   petición HTTP
     * @return respuesta estandarizada
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {

        log.error("Error inesperado durante el procesamiento de la solicitud.", exception);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "Ha ocurrido un error interno. Intente nuevamente más tarde.",
                request
        );
    }

    /**
     * Construye una respuesta HTTP utilizando el contrato estándar de errores.
     *
     * @param status    estado HTTP
     * @param errorCode código de error
     * @param message   mensaje descriptivo
     * @param request   petición HTTP
     * @return respuesta HTTP
     */
    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            HttpServletRequest request) {

        return ResponseEntity.status(status)
                .body(buildApiError(status, errorCode, message, request));

    }

    /**
     * Construye una instancia de {@link ApiError}.
     *
     * @param status    estado HTTP
     * @param errorCode código de error
     * @param message   mensaje descriptivo
     * @param request   petición HTTP
     * @return objeto de respuesta de error
     */
    private ApiError buildApiError(
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            HttpServletRequest request) {

        return ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(errorCode.name())
                .message(message)
                .path(request.getRequestURI())
                .build();
    }

    /**
     * Formatea un error de validación de un campo.
     *
     * @param fieldError error de validación
     * @return mensaje formateado
     */
    private String formatFieldError(FieldError fieldError) {

        return "%s: %s".formatted(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }

}