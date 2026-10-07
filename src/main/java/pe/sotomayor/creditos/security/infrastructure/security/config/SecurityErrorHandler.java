package pe.sotomayor.creditos.security.infrastructure.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import org.springframework.stereotype.Component;

import pe.sotomayor.creditos.shared.response.ApiError;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Traduce fallos de autenticación y autorización previos a Spring MVC a ApiError.
 *
 * <p>Se agrega porque el advice de controladores no gestiona automáticamente
 * las excepciones de los filtros. No envía al cliente detalles internos de la causa.</p>
 *
 * @author DarwinG
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    /**
     * Serializador configurado por la aplicación, reutilizado para mantener el JSON de errores.
     */
    private final ObjectMapper objectMapper;

    /**
     * Responde 401 y anuncia el esquema Bearer cuando no hay autenticación válida.
     *
     * @param req solicitud cuyo URI identifica el recurso
     * @param res respuesta que recibirá estado, cabeceras y JSON
     * @param ex  causa de autenticación, cuyo detalle no se expone
     * @throws IOException si falla la escritura del cuerpo de respuesta
     */
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res,
                         AuthenticationException ex) throws IOException {
        res.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(req, res, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Autenticación requerida o sesión inválida.");
    }

    /**
     * Responde 403 cuando existe autenticación pero falta autorización.
     *
     * @param req solicitud rechazada
     * @param res respuesta JSON de acceso denegado
     * @param ex  causa de autorización, no expuesta al cliente
     * @throws IOException si falla la escritura del cuerpo de respuesta
     */
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res,
                       AccessDeniedException ex) throws IOException {
        write(req, res, HttpStatus.FORBIDDEN, "FORBIDDEN",
                "No tiene permiso para realizar esta operación.");
    }

    /**
     * Responde 503 cuando el filtro detecta un fallo de acceso a datos.
     *
     * @param req solicitud que no pudo autenticarse por una falla técnica
     * @param res respuesta que informa indisponibilidad temporal
     * @throws IOException si falla la escritura del cuerpo de respuesta
     */
    public void unavailable(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        write(req, res, HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                "El servicio no está disponible temporalmente.");
    }

    /**
     * Construye el contrato de error y evita su almacenamiento por cachés HTTP.
     *
     * @param req     solicitud utilizada para obtener el URI
     * @param res     respuesta que se completará; aún no debe estar comprometida
     * @param status  estado HTTP correspondiente a la categoría del fallo
     * @param code    identificador funcional consumible por el frontend
     * @param message texto seguro, sin token, contraseña ni detalles internos
     * @throws IOException si el serializador no puede escribir la respuesta
     */
    private void write(HttpServletRequest req, HttpServletResponse res,
                       HttpStatus status, String code, String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        objectMapper.writeValue(res.getOutputStream(), ApiError.builder()
                .timestamp(LocalDateTime.now()).status(status.value())
                .error(status.getReasonPhrase()).errorCode(code)
                .message(message).path(req.getRequestURI()).build());
    }
}
