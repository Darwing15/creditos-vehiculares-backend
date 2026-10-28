package pe.sotomayor.creditos.customers.infrastructure.config;

import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pe.sotomayor.creditos.customers.domain.service.IdentityLookupPolicy;

/**
 * Propiedades externas de Factiliza; el token no forma parte de toString ni de la API.
 *
 * @author DarwinG
 */
@ConfigurationProperties(prefix = "app.identity-lookup")
public class IdentityLookupProperties implements IdentityLookupPolicy {

    private boolean enabled;
    private String token = "";
    private Duration cacheTtl = Duration.ofDays(30);
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(5);
    private int monthlyLimit = 100;
    private int userPerMinuteLimit = 10;
    private int globalPerMinuteLimit = 30;
    private ZoneId quotaZone = ZoneId.of("America/Lima");

    public boolean enabled() {
        return enabled;
    }

    public Duration cacheTtl() {
        return cacheTtl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String value) {
        token = value;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration value) {
        cacheTtl = value;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration value) {
        connectTimeout = value;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration value) {
        readTimeout = value;
    }

    public int getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(int value) {
        monthlyLimit = value;
    }

    public int getUserPerMinuteLimit() {
        return userPerMinuteLimit;
    }

    public void setUserPerMinuteLimit(int value) {
        userPerMinuteLimit = value;
    }

    public int getGlobalPerMinuteLimit() {
        return globalPerMinuteLimit;
    }

    public void setGlobalPerMinuteLimit(int value) {
        globalPerMinuteLimit = value;
    }

    public ZoneId getQuotaZone() {
        return quotaZone;
    }

    public void setQuotaZone(ZoneId value) {
        quotaZone = value;
    }

    /**
     * Valida al arrancar; admite token vacío únicamente cuando el consumo está deshabilitado.
     */
    public void validate() {
        if (cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero() || cacheTtl.compareTo(Duration.ofDays(365)) > 0
                || connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()
                || connectTimeout.compareTo(Duration.ofSeconds(10)) > 0
                || readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()
                || readTimeout.compareTo(Duration.ofSeconds(10)) > 0
                || monthlyLimit < 0 || userPerMinuteLimit < 1 || globalPerMinuteLimit < 1 || quotaZone == null)
            throw new IllegalStateException("Revise las propiedades de consulta de DNI.");
        if (enabled && (token == null || token.isBlank() || !token.equals(token.strip())
                || token.contains("\r") || token.contains("\n") || token.startsWith("Bearer ")))
            throw new IllegalStateException("Configure el token de Factiliza sin el prefijo Bearer.");
    }
}

