package pe.sotomayor.creditos.customers.integration;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import pe.sotomayor.creditos.support.PostgresTestContainer;

import pe.sotomayor.creditos.customers.domain.repository.IdentityDocumentCacheRepository;
import pe.sotomayor.creditos.customers.domain.model.entity.IdentityDocumentCache;
import pe.sotomayor.creditos.customers.domain.model.valueobject.IdentityData;

import pe.sotomayor.creditos.customers.infrastructure.config.IdentityLookupProperties;
import pe.sotomayor.creditos.customers.infrastructure.identity.PostgresIdentityLookupBudget;

import pe.sotomayor.creditos.customers.domain.exception.IdentityLookupException;

import pe.sotomayor.creditos.shared.error.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DataJpaTest(properties = "spring.datasource.hikari.connection-init-sql=SET TIME ZONE 'America/Lima'")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestContainer.class, PostgresIdentityLookupBudget.class,
        IdentityDocumentLookupPersistenceTest.PropertiesConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class IdentityDocumentLookupPersistenceTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class PropertiesConfig {
        @Bean
        IdentityLookupProperties lookupProperties() {
            return new IdentityLookupProperties();
        }
    }

    @Autowired
    IdentityDocumentCacheRepository repository;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PostgresIdentityLookupBudget budget;

    @Autowired
    IdentityLookupProperties properties;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM identity_document_cache");
        jdbc.update("DELETE FROM identity_lookup_usage");
        properties.setMonthlyLimit(100);
        properties.setUserPerMinuteLimit(10);
        properties.setGlobalPerMinuteLimit(30);
    }

    @Test
    void shouldReplaceResultWhenSameDniIsStoredAgain() {

        /* Arrange */

        var now = createTime();
        var first = IdentityDocumentCache.create(
                createData(), now, now.plusSeconds(60), 7L);
        repository.upsert(first);
        var newer = IdentityDocumentCache.create(
                new IdentityData(
                        "00000001",
                        "NUEVO",
                        "UNO",
                        "DOS",
                        "FACTILIZA"
                ),
                now.plusSeconds(10),
                now.plusSeconds(70),
                8L
        );

        /* Act */

        repository.upsert(newer);
        var result = repository.findByDocumentNumber("00000001")
                .orElseThrow();

        /* Assert */

        assertThat(result.getFullName())
                .isEqualTo("NUEVO UNO DOS");
        assertThat(result.getCreatedBy())
                .isEqualTo(7L);
        assertThat(result.getUpdatedBy())
                .isEqualTo(8L);
        assertThat(result.getConsultedAt())
                .isEqualTo(now.plusSeconds(10));
        assertThat(jdbc.queryForObject(
                "SELECT created_at FROM identity_document_cache",
                OffsetDateTime.class).toInstant())
                .isEqualTo(now);
        assertThat(jdbc.queryForObject(
                "SELECT updated_at FROM identity_document_cache",
                OffsetDateTime.class).toInstant())
                .isEqualTo(now.plusSeconds(10));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM identity_document_cache",
                Long.class)).isEqualTo(1L);
    }

    @Test
    void shouldPreserveNewerResultWhenOlderCallFinishesLater() {

        /* Arrange */

        var now = createTime();
        repository.upsert(IdentityDocumentCache.create(
                createData(),
                now.plusSeconds(10),
                now.plusSeconds(70),
                7L)
        );
        var older = IdentityDocumentCache.create(
                new IdentityData(
                        "00000001",
                        "ANTERIOR",
                        "UNO",
                        "DOS",
                        "FACTILIZA"
                ),
                now,
                now.plusSeconds(60),
                8L
        );

        /* Act */

        repository.upsert(older);

        /* Assert */

        assertThat(repository.findByDocumentNumber("00000001")
                .orElseThrow().getFullName())
                .isEqualTo("NOMBRES UNO DOS");
    }

    @Test
    void shouldRejectOnlyExcessCallsWhenReservationsCompete() throws Exception {

        /* Arrange */

        var now = createTime();
        properties.setMonthlyLimit(3);
        var actions = new ArrayList<Callable<Boolean>>();
        for (int i = 0; i < 12; i++)
            actions.add(() -> {
                try {
                    budget.reserve(7L, now);
                    return true;
                } catch (IdentityLookupException failure) {
                    if (failure.getErrorCode() != ErrorCode.IDENTITY_QUOTA_EXHAUSTED) throw failure;
                    return false;
                }
            });

        /* Act */

        long successes = 0;
        try (var executor = Executors.newFixedThreadPool(6)) {
            for (var result : executor.invokeAll(actions)) if (result.get()) successes++;
        }

        /* Assert */

        assertThat(successes).isEqualTo(3);
        assertThat(jdbc.queryForObject(
                "SELECT request_count FROM identity_lookup_usage WHERE bucket_key = 'month:2026-10'",
                Long.class)).isEqualTo(3L);
    }

    @Test
    void shouldRollbackMonthlyReservationWhenUserRateLimitIsReached() {

        /* Arrange */

        var now = createTime();
        properties.setUserPerMinuteLimit(1);
        budget.reserve(7L, now);

        /* Act */

        var failure = catchThrowable(() -> budget.reserve(7L, now));

        /* Assert */

        assertThat(failure)
                .isInstanceOf(IdentityLookupException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDENTITY_RATE_LIMITED);
        assertThat(jdbc.queryForObject(
                "SELECT request_count FROM identity_lookup_usage WHERE bucket_key = 'month:2026-10'",
                Long.class)).isEqualTo(1L);
    }

    @Test
    void shouldUseNewPeriodWhenMonthChangesInConfiguredZone() {

        /* Arrange */

        properties.setMonthlyLimit(1);
        budget.reserve(7L, Instant.parse("2026-11-01T04:59:59Z"));

        /* Act */

        budget.reserve(7L, Instant.parse("2026-11-01T05:00:00Z"));

        /* Assert */

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM identity_lookup_usage WHERE bucket_key LIKE 'month:%'",
                Long.class)).isEqualTo(2L);
    }

    private IdentityData createData() {
        return new IdentityData(
                "00000001",
                "NOMBRES",
                "UNO",
                "DOS",
                "FACTILIZA"
        );
    }

    private Instant createTime() {
        return Instant.parse("2026-10-11T15:00:00Z");
    }
}

