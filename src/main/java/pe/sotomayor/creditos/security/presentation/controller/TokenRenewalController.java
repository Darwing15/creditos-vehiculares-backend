package pe.sotomayor.creditos.security.presentation.controller;

import io.jsonwebtoken.JwtException;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.UnauthorizedException;

/**
 * Renovacion desde un token de acceso valido; la cadena vuelve a comprobar la cuenta.
 *
 * @author DarwinG
 */
@RestController
@RequestMapping("/api/auth")
public class TokenRenewalController {
    private final JwtService jwtService;

    public TokenRenewalController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/renew")
    public ResponseEntity<RenewResponse> renew(
            @AuthenticationPrincipal SecurityPrincipal principal,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        try {
            var renewed = jwtService.renewToken(
                    principal, authorization.substring(7));
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(new RenewResponse(
                            renewed.token(), "Bearer", renewed.expiresIn()));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED, "Sesión inválida o expirada.");
        }
    }

    public record RenewResponse(String token, String tokenType, long expiresIn) {
    }
}
