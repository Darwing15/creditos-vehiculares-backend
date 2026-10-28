package pe.sotomayor.creditos.customers.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.sotomayor.creditos.customers.application.dto.request.IdentityLookupRequest;
import pe.sotomayor.creditos.customers.application.dto.response.IdentityLookupResponse;

import pe.sotomayor.creditos.customers.application.usecase.LookupCustomerIdentityUseCase;
import pe.sotomayor.creditos.shared.response.ApiError;

/**
 * Consulta autenticada separada del alta; datos personales fuera de la URL y sin cache HTTP.
 *
 * @author DarwinG
 */
@Tag(name = "Customers", description = "Clientes y consulta documental autenticada.")
@RestController
@RequestMapping("/api/customers/identity-lookups")
@RequiredArgsConstructor
public class CustomerIdentityLookupController {

    private final LookupCustomerIdentityUseCase lookup;

    /**
     * @return resultado minimo; requiere customers:write en ruta y caso de uso.
     */
    @Operation(
            operationId = "lookupCustomerIdentity",
            summary = "Consultar nombres asociados a un DNI",
            description = """
                    Requiere JWT interno y permiso customers:write. Solo admite DNI textual de ocho dígitos ASCII,
                    sin espacios; CE no está habilitado. Actor y sucursal proceden de la sesión autenticada.
                    Reutiliza caché vigente o reserva presupuesto local y consulta Factiliza. Con enabled=false
                    todavía puede reutilizar caché vigente. El token externo permanece exclusivamente en el backend.
                    Esta operación no crea ni reserva un cliente, no verifica titularidad ni aprueba un crédito.
                    Una consulta fallida puede consumir presupuesto si ya se inició el intento externo.
                    Los ejemplos son sintéticos y no autorizan consultas reales al proveedor.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "DNI para consultar. No enviar actor, sucursal, proveedor ni token de Factiliza.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = IdentityLookupRequest.class),
                            examples = @ExampleObject(
                                    name = "DniSintetico",
                                    value = """
                                            {"documentType":"DNI","documentNumber":"00000001"}
                                            """
                            )
                    )
            )
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resultado mínimo de DNI obtenido de caché vigente o de Factiliza. No crea un cliente ni acredita titularidad.",
                    headers = @Header(
                            name = "Cache-Control",
                            description = "Impide almacenar la respuesta exitosa en cachés HTTP.",
                            schema = @Schema(
                                    type = "string",
                                    allowableValues = {"no-store"},
                                    example = "no-store"
                            )
                    ),
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = IdentityLookupResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "ResultadoExterno",
                                            summary = "Respuesta sintética de consulta externa; no ejecutar contra datos reales.",
                                            value = """
                                                    {
                                                      "documentType": "DNI",
                                                      "documentNumber": "00000001",
                                                      "givenNames": "NOMBRES",
                                                      "firstSurname": "PRUEBA",
                                                      "secondSurname": null,
                                                      "fullName": "NOMBRES PRUEBA",
                                                      "provider": "FACTILIZA",
                                                      "fromCache": false,
                                                      "consultedAt": 1793106000,
                                                      "expiresAt": 1795698000
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "ResultadoEnCache",
                                            summary = "Resultado vigente reutilizado; no consume una llamada externa.",
                                            value = """
                                                    {
                                                      "documentType": "DNI",
                                                      "documentNumber": "00000001",
                                                      "givenNames": "NOMBRES",
                                                      "firstSurname": "PRUEBA",
                                                      "secondSurname": null,
                                                      "fullName": "NOMBRES PRUEBA",
                                                      "provider": "FACTILIZA",
                                                      "fromCache": true,
                                                      "consultedAt": 1793106000,
                                                      "expiresAt": 1795698000
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Solicitud inválida: JSON, campos obligatorios, formato o tipo distinto de DNI.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "ValidacionHttp",
                                            summary = "Bean Validation o JSON inválido.",
                                            value = """
                                                    {
                                                      "timestamp": [
                                                        2026,
                                                        10,
                                                        27,
                                                        8,
                                                        0
                                                      ],
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "errorCode": "VALIDATION_ERROR",
                                                      "message": "El cuerpo de la solicitud contiene un formato inválido.",
                                                      "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "DocumentoNoAdmitido",
                                            summary = "El caso de uso rechaza CE o un DNI inválido.",
                                            value = """
                                                    {
                                                      "timestamp": [
                                                        2026,
                                                        10,
                                                        27,
                                                        8,
                                                        0
                                                      ],
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "errorCode": "IDENTITY_DOCUMENT_INVALID",
                                                      "message": "La consulta requiere un DNI de ocho dígitos.",
                                                      "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Autenticación interna ausente o inválida. No representa un fallo de credenciales de Factiliza.",
                    headers = @Header(
                            name = "WWW-Authenticate",
                            description = "Esquema requerido por el rechazo del filtro JWT.",
                            schema = @Schema(type = "string", example = "Bearer")
                    ),
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "SesionInvalida",
                                            summary = "Rechazo por el filtro de seguridad.",
                                            value = """
                                                    {
                                                      "timestamp": [
                                                        2026,
                                                        10,
                                                        27,
                                                        8,
                                                        0
                                                      ],
                                                      "status": 401,
                                                      "error": "Unauthorized",
                                                      "errorCode": "UNAUTHORIZED",
                                                      "message": "Autenticación requerida o sesión inválida.",
                                                      "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "La sesión interna no tiene customers:write o el contexto no permite la operación.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "PermisoInsuficiente",
                                            summary = "Autorización interna denegada.",
                                            value = """
                                                    {
                                                    "timestamp": [
                                                      2026,
                                                      10,
                                                      27,
                                                      8,
                                                      0
                                                    ],
                                                    "status": 403,
                                                    "error": "Forbidden",
                                                    "errorCode": "FORBIDDEN",
                                                    "message": "No tiene permiso para realizar esta operación.",
                                                    "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "El proveedor informa explícitamente que no encontró datos del DNI.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "DniSinResultado",
                                            summary = "Ausencia de resultado informada por el proveedor.",
                                            value = """
                                                    {
                                                      "timestamp": [
                                                        2026,
                                                        10,
                                                        27,
                                                        8,
                                                        0
                                                      ],
                                                      "status": 404,
                                                      "error": "Not Found",
                                                      "errorCode": "IDENTITY_NOT_FOUND",
                                                      "message": "No se encontraron datos para este DNI.",
                                                      "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto técnico de integridad al persistir. No significa que exista un cliente con ese DNI.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "ConflictoPersistencia",
                                            summary = "Contrato genérico de DataIntegrityViolationException.",
                                            value = """
                                                    {
                                                      "timestamp": [
                                                        2026,
                                                        10,
                                                        27,
                                                        8,
                                                        0
                                                      ],
                                                      "status": 409,
                                                      "error": "Conflict",
                                                      "errorCode": "DATA_INTEGRITY_CONFLICT",
                                                      "message": "La operación entra en conflicto con los datos existentes.",
                                                      "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Límite de frecuencia local, espera de coordinación agotada o 429 del proveedor. No se garantiza Retry-After.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "LimiteFrecuencia",
                                            summary = "Límite local o externo alcanzado.",
                                            value = """
                                                    {
                                                      "timestamp": [
                                                        2026,
                                                        10,
                                                        27,
                                                        8,
                                                        0
                                                      ],
                                                      "status": 429,
                                                      "error": "Too Many Requests",
                                                      "errorCode": "IDENTITY_RATE_LIMITED",
                                                      "message": "Espera un momento antes de volver a consultar el DNI.",
                                                      "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Fallo interno inesperado traducido por GlobalExceptionHandler, sin detalles internos.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "ErrorInterno",
                                            summary = "Fallo no controlado.",
                                            value = """
                                                    {
                                                      "timestamp": [
                                                        2026,
                                                        10,
                                                        27,
                                                        8,
                                                        0
                                                      ],
                                                      "status": 500,
                                                      "error": "Internal Server Error",
                                                      "errorCode": "INTERNAL_ERROR",
                                                      "message": "Ha ocurrido un error interno. Intente nuevamente más tarde.",
                                                      "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Respuesta externa inválida o incoherente: JSON, estado, documento, nombres o tamaño.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "RespuestaInvalida",
                                            summary = "El resultado externo no puede aceptarse.",
                                            value = """
                                                    {
                                                      "timestamp": [
                                                        2026,
                                                        10,
                                                        27,
                                                        8,
                                                        0
                                                      ],
                                                      "status": 502,
                                                      "error": "Bad Gateway",
                                                      "errorCode": "IDENTITY_PROVIDER_INVALID_RESPONSE",
                                                      "message": "El servicio devolvió datos que no pudieron validarse.",
                                                      "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Consulta externa deshabilitada, presupuesto mensual local agotado o indisponibilidad de proveedor/infraestructura. Un 401/403 externo se traduce aquí, sin invalidar la sesión del operador.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "ConsultaDeshabilitada",
                                            summary = "Una caché vigente todavía puede responder con 200.",
                                            value = """
                                                    {
                                                      "timestamp": [
                                                        2026,
                                                        10,
                                                        27,
                                                        8,
                                                        0
                                                      ],
                                                      "status": 503,
                                                      "error": "Service Unavailable",
                                                      "errorCode": "IDENTITY_LOOKUP_DISABLED",
                                                      "message": "La consulta de DNI no está habilitada.",
                                                      "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "CuotaLocalAgotada",
                                            summary = "El límite es local; no describe el saldo real de Factiliza.",
                                            value = """
                                                    {
                                                      "timestamp": [
                                                        2026,
                                                        10,
                                                        27,
                                                        8,
                                                        0
                                                      ],
                                                      "status": 503,
                                                      "error": "Service Unavailable",
                                                      "errorCode": "IDENTITY_QUOTA_EXHAUSTED",
                                                      "message": "Se alcanzó el límite configurado de consultas de DNI.",
                                                      "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "ProveedorIndisponible",
                                            summary = "Fallo controlado de proveedor o reserva.",
                                            value = """
                                                    {
                                                      "timestamp": [
                                                        2026,
                                                        10,
                                                        27,
                                                        8,
                                                        0
                                                      ],
                                                      "status": 503,
                                                      "error": "Service Unavailable",
                                                      "errorCode": "IDENTITY_PROVIDER_UNAVAILABLE",
                                                      "message": "No se pudo consultar el DNI en este momento.",
                                                      "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "AutenticacionIndisponible",
                                            summary = "Fallo de acceso a datos detectado por el filtro JWT.",
                                            value = """
                                                    {
                                                      "timestamp": [
                                                        2026,
                                                        10,
                                                        27,
                                                        8,
                                                        0
                                                      ],
                                                      "status": 503,
                                                      "error": "Service Unavailable",
                                                      "errorCode": "SERVICE_UNAVAILABLE",
                                                      "message": "El servicio no está disponible temporalmente.",
                                                      "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "504",
                    description = "Timeout de transporte o estado 408/504 informado por el proveedor.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "TiempoAgotado",
                                            summary = "Tiempo máximo de consulta agotado.",
                                            value = """
                                                    {
                                                      "timestamp": [
                                                        2026,
                                                        10,
                                                        27,
                                                        8,
                                                        0
                                                      ],
                                                      "status": 504,
                                                      "error": "Gateway Timeout",
                                                      "errorCode": "IDENTITY_PROVIDER_TIMEOUT",
                                                      "message": "La consulta de DNI tardó demasiado.",
                                                      "path": "/api/customers/identity-lookups"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    @PostMapping
    public ResponseEntity<IdentityLookupResponse> lookup(@Valid @RequestBody IdentityLookupRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(lookup.lookup(request));
    }
}

