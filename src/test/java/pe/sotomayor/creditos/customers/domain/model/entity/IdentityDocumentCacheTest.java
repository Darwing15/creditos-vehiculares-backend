package pe.sotomayor.creditos.customers.domain.model.entity;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import pe.sotomayor.creditos.customers.domain.model.valueobject.IdentityData;
import pe.sotomayor.creditos.customers.domain.exception.IdentityLookupException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class IdentityDocumentCacheTest {

    @Test
    void shouldExpireEntryWhenCurrentTimeReachesExpiration() {

        /* Arrange */

        var now = createTime();
        var entry = IdentityDocumentCache.create(
                createData(),
                now,
                now.plusSeconds(60),
                7L
        );

        /* Act */

        var atBoundary = entry.isFresh(now.plusSeconds(60));

        /* Assert */

        assertThat(entry.isFresh(now.plusSeconds(59))).isTrue();
        assertThat(atBoundary).isFalse();
    }

    @Test
    void shouldRejectEntryWhenExpirationIsNotAfterConsultation() {

        /* Arrange */

        var now = createTime();
        var data = createData();

        /* Act */

        var failure = catchThrowable(() -> IdentityDocumentCache.create(data, now, now, 7L));

        /* Assert */

        assertThat(failure).isInstanceOf(IdentityLookupException.class);
    }

    private IdentityData createData() {
        return new IdentityData(
                "00000001",
                "NOMBRES",
                "UNO",
                null,
                "FACTILIZA"
        );
    }

    private Instant createTime() {
        return Instant.parse("2026-10-11T15:00:00Z");
    }
}

