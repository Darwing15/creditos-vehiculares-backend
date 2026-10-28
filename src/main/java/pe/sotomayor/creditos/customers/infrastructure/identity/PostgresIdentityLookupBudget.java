package pe.sotomayor.creditos.customers.infrastructure.identity;

import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

import java.sql.Timestamp;

import org.springframework.stereotype.Component;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import org.springframework.dao.DataAccessException;

import pe.sotomayor.creditos.customers.domain.service.IdentityLookupBudget;
import pe.sotomayor.creditos.customers.domain.exception.IdentityLookupException;
import pe.sotomayor.creditos.customers.infrastructure.config.IdentityLookupProperties;

/**
 * Contadores tecnicos compartidos entre instancias; reserva atomica con transaccion corta.
 *
 * @author DarwinG
 */
@Component
public class PostgresIdentityLookupBudget implements IdentityLookupBudget {

    private static final String RESERVE = """
            INSERT INTO identity_lookup_usage (bucket_key, request_count, expires_at)
            VALUES (?, 1, ?)
            ON CONFLICT (bucket_key) DO UPDATE
                SET request_count = identity_lookup_usage.request_count + 1
            WHERE identity_lookup_usage.request_count < ?
            """;

    private final JdbcTemplate jdbc;
    private final IdentityLookupProperties properties;
    private final TransactionTemplate transaction;

    /**
     * Configura una reserva independiente que se confirma antes del intento externo.
     */
    public PostgresIdentityLookupBudget(
            JdbcTemplate jdbc,
            IdentityLookupProperties properties,
            PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transaction.setTimeout(5);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reserve(Long actorId, Instant now) {
        if (actorId == null || actorId <= 0 || now == null)
            throw IdentityLookupException.unavailable();
        var month = YearMonth.from(now.atZone(properties.getQuotaZone()));
        Instant monthEnd = month.plusMonths(1).atDay(1)
                .atStartOfDay(properties.getQuotaZone()).toInstant();
        Instant minute = now.truncatedTo(ChronoUnit.MINUTES);
        try {
            transaction.executeWithoutResult(status -> {
                // Orden unico de bloqueos en todas las instancias para evitar interbloqueos.
                take("month:" + month, monthEnd, properties.getMonthlyLimit(), true);
                take("global-minute:" + minute.getEpochSecond(), minute.plusSeconds(60),
                        properties.getGlobalPerMinuteLimit(), false);
                take("user-minute:" + actorId + ":" + minute.getEpochSecond(), minute.plusSeconds(60),
                        properties.getUserPerMinuteLimit(), false);
            });
        } catch (DataAccessException failure) {
            throw IdentityLookupException.unavailable();
        }
    }

    private void take(String key, Instant expiration, int limit, boolean monthly) {
        if (limit <= 0 || jdbc.update(RESERVE, key, Timestamp.from(expiration), limit) != 1) {
            if (monthly) throw IdentityLookupException.quota();
            throw IdentityLookupException.rateLimited();
        }
    }
}

