package pe.sotomayor.creditos.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
     * Maneja solicitudes que omiten un parámetro HTTP obligatorio.
     *
     * @param exception excepción producida por Spring MVC
     * @param request   petición HTTP
     * @return respuesta estandarizada con código HTTP 400
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {

        String message =
                "El parámetro requerido '%s' no está presente."
                        .formatted(exception.getParameterName());

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
     * Maneja parámetros HTTP cuyo valor no puede convertirse
     * al tipo requerido por el endpoint.
     *
     * @param exception excepción producida por Spring MVC
     * @param request   petición HTTP
     * @return respuesta estandarizada con código HTTP 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {

        String message =
                "El parámetro '%s' contiene un valor inválido."
                        .formatted(exception.getName());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                message,
                request
        );
    }

    /**
     * Clasifica fallos de credenciales/estado como 401 y del proveedor como 503.
     *
     * <p>Reemplaza el handler limitado a BadCredentialsException para no convertir
     * cuentas deshabilitadas en un falso error 500. No expone la causa interna.</p>
     *
     * @param exception causa recibida por el advice
     * @param request   solicitud que aporta la ruta al error
     * @return respuesta ApiError con el estado correspondiente a esta categoría
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request) {
        if (exception instanceof
                AuthenticationServiceException) {
            log.error("Fallo del servicio de autenticación.", exception);
            return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.INTERNAL_ERROR,
                    "El servicio no está disponible temporalmente.", request);
        }
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.INVALID_CREDENTIALS,
                "Invalid credentials.",
                request
        );
    }

    /**
     * Traduce a 403 las denegaciones de autorización que llegan a Spring MVC.
     *
     * @param exception causa recibida por el advice
     * @param request   solicitud que aporta la ruta al error
     * @return respuesta ApiError con el estado correspondiente a esta categoría
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(
            AccessDeniedException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN,
                "No tiene permiso para realizar esta operación.", request);
    }

    /**
     * Informa un conflicto 409 cuando la escritura usa una versión JPA obsoleta.
     *
     * <p>No reintenta automáticamente operaciones sensibles con datos anteriores.</p>
     *
     * @param exception causa recibida por el advice
     * @param request   solicitud que aporta la ruta al error
     * @return respuesta ApiError con el estado correspondiente a esta categoría
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleConcurrentUpdate(
            OptimisticLockingFailureException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ErrorCode.CONCURRENT_MODIFICATION,
                "El registro cambió. Recargue antes de volver a guardar.", request);
    }

    /**
     * Devuelve el conflicto de integridad mediante un contrato seguro sin SQL.
     *
     * <p>Es una traducción genérica inicial; no identifica toda infracción como
     * duplicado. Las restricciones conocidas pueden recibir códigos más específicos.</p>
     *
     * @param exception causa recibida por el advice
     * @param request   solicitud que aporta la ruta al error
     * @return respuesta ApiError con el estado correspondiente a esta categoría
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ErrorCode.DATA_INTEGRITY_CONFLICT,
                "La operación entra en conflicto con los datos existentes.", request);
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
     * @param exception excepción producida por Spring MVC
     * @param request   solicitud HTTP
     * @return respuesta de recurso no encontrado
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND,
                "El recurso solicitado no existe.",
                request
        );
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