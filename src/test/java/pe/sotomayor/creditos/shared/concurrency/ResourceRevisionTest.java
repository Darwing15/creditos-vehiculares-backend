package pe.sotomayor.creditos.shared.concurrency;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import pe.sotomayor.creditos.shared.exception.ConflictException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

class ResourceRevisionTest {

    @Test
    void decimalScaleDoesNotInventAConflict() {

        /* Arrange */

        /* Act */

        /* Assert */

        assertThat(ResourceRevision.of(new BigDecimal("100.00")))
                .isEqualTo(ResourceRevision.of(new BigDecimal("100.0")));
    }

    @Test
    void fieldBoundariesAndNullsCannotCollide() {

        /* Arrange */

        /* Act */

        /* Assert */

        assertThat(ResourceRevision.of("ab", "c"))
                .isNotEqualTo(ResourceRevision.of("a", "bc"));
        assertThat(ResourceRevision.of((Object) null))
                .isNotEqualTo(ResourceRevision.of(""));
        assertThat(ResourceRevision.of("-1:"))
                .isNotEqualTo(ResourceRevision.of((Object) null));
    }

    @Test
    void onlyMatchingRevisionCanUpdate() {

        /* Arrange */

        /* Act */

        String current = ResourceRevision.of("Cliente", true);

        Throwable throwablea = catchThrowable(
                () -> ResourceRevision.requireMatch(null, current));

        Throwable throwableb = catchThrowable(
                () -> ResourceRevision.requireMatch(ResourceRevision.of("Otro", true), current));

        /* Assert */

        assertThatCode(() -> ResourceRevision.requireMatch(current, current))
                .doesNotThrowAnyException();
        assertThat(throwablea)
                .isInstanceOf(ConflictException.class);
        assertThat(throwableb)
                .isInstanceOf(ConflictException.class);
    }
}

