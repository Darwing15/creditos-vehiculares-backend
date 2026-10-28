package pe.sotomayor.creditos.customers.domain.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class IdentityLookupCoordinatorTest {

    @Test
    void shouldSerializeCallsWhenDocumentIsTheSame() throws Exception {

        /* Arrange */

        var coordinator = new IdentityLookupCoordinator();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        var calls = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> coordinator.execute("00000001", () -> {
                calls.incrementAndGet();
                entered.countDown();
                try {
                    if (!release.await(3, TimeUnit.SECONDS))
                        throw new IllegalStateException("Release deadline");
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(failure);
                }
                return "FIRST";
            }));
            entered.await(3, TimeUnit.SECONDS);
            var second = executor.submit(() -> {
                secondStarted.countDown();
                return coordinator.execute("00000001", () -> {
                    calls.incrementAndGet();
                    return "SECOND";
                });
            });
            secondStarted.await(3, TimeUnit.SECONDS);

            /* Act */

            int beforeRelease = calls.get();
            release.countDown();
            var firstResult = first.get(3, TimeUnit.SECONDS);
            var secondResult = second.get(3, TimeUnit.SECONDS);

            /* Assert */

            assertThat(beforeRelease).isEqualTo(1);
            assertThat(firstResult).isEqualTo("FIRST");
            assertThat(secondResult).isEqualTo("SECOND");
            assertThat(calls.get()).isEqualTo(2);
        } finally {
            release.countDown();
        }
    }

    @Test
    void shouldReleaseLockWhenActionFails() {

        /* Arrange */

        var coordinator = new IdentityLookupCoordinator();

        /* Act */

        var failure = catchThrowable(() -> coordinator.execute("00000001", () -> {
            throw new IllegalStateException("Synthetic failure");
        }));

        var result = coordinator.execute("00000001", () -> "RECOVERED");

        /* Assert */

        assertThat(failure).isInstanceOf(IllegalStateException.class);
        assertThat(result).isEqualTo("RECOVERED");
    }
}
