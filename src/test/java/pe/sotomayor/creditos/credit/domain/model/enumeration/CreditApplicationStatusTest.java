package pe.sotomayor.creditos.credit.domain.model.enumeration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreditApplicationStatusTest {

    @Test
    void shouldAllowTransitionFromPendingToApproved() {

        /* Arrange */

        CreditApplicationStatus current =
                CreditApplicationStatus.PENDING;

        /* Act */

        boolean allowed =
                current.canTransitionTo(
                        CreditApplicationStatus.APPROVED
                );

        /* Assert */

        assertThat(allowed)
                .isTrue();
    }

    @Test
    void shouldAllowTransitionFromPendingToRejected() {

        /* Arrange */

        CreditApplicationStatus current =
                CreditApplicationStatus.PENDING;

        /* Act */

        boolean allowed =
                current.canTransitionTo(
                        CreditApplicationStatus.REJECTED
                );

        /* Assert */

        assertThat(allowed)
                .isTrue();
    }

    @Test
    void shouldAllowTransitionFromPendingToCancelled() {

        /* Arrange */

        CreditApplicationStatus current =
                CreditApplicationStatus.PENDING;

        /* Act */

        boolean allowed =
                current.canTransitionTo(
                        CreditApplicationStatus.CANCELLED
                );

        /* Assert */

        assertThat(allowed)
                .isTrue();
    }

    @Test
    void shouldAllowTransitionFromPendingToExpired() {

        /* Arrange */

        CreditApplicationStatus current =
                CreditApplicationStatus.PENDING;

        /* Act */

        boolean allowed =
                current.canTransitionTo(
                        CreditApplicationStatus.EXPIRED
                );

        /* Assert */

        assertThat(allowed)
                .isTrue();
    }

    @Test
    void shouldRejectTransitionFromPendingToPending() {

        /* Arrange */

        CreditApplicationStatus current =
                CreditApplicationStatus.PENDING;

        /* Act */

        boolean allowed =
                current.canTransitionTo(
                        CreditApplicationStatus.PENDING
                );

        /* Assert */

        assertThat(allowed)
                .isFalse();
    }

    @Test
    void shouldRejectTransitionFromApprovedToAnyState() {

        /* Arrange */

        CreditApplicationStatus current =
                CreditApplicationStatus.APPROVED;

        /* Act & Assert */

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.PENDING
                )
        ).isFalse();

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.REJECTED
                )
        ).isFalse();

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.CANCELLED
                )
        ).isFalse();

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.EXPIRED
                )
        ).isFalse();
    }

    @Test
    void shouldRejectTransitionFromRejectedToAnyState() {

        /* Arrange */

        CreditApplicationStatus current =
                CreditApplicationStatus.REJECTED;

        /* Act & Assert */

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.PENDING
                )
        ).isFalse();

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.APPROVED
                )
        ).isFalse();

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.CANCELLED
                )
        ).isFalse();

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.EXPIRED
                )
        ).isFalse();
    }

    @Test
    void shouldRejectTransitionFromCancelledToAnyState() {

        /* Arrange */

        CreditApplicationStatus current =
                CreditApplicationStatus.CANCELLED;

        /* Act & Assert */

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.PENDING
                )
        ).isFalse();

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.APPROVED
                )
        ).isFalse();

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.REJECTED
                )
        ).isFalse();

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.EXPIRED
                )
        ).isFalse();
    }

    @Test
    void shouldRejectTransitionFromExpiredToAnyState() {

        /* Arrange */

        CreditApplicationStatus current =
                CreditApplicationStatus.EXPIRED;

        /* Act & Assert */

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.PENDING
                )
        ).isFalse();

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.APPROVED
                )
        ).isFalse();

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.REJECTED
                )
        ).isFalse();

        assertThat(
                current.canTransitionTo(
                        CreditApplicationStatus.CANCELLED
                )
        ).isFalse();
    }

    @Test
    void shouldRejectTransitionToNull() {

        /* Arrange */

        CreditApplicationStatus current =
                CreditApplicationStatus.PENDING;

        /* Act */

        boolean allowed =
                current.canTransitionTo(null);

        /* Assert */

        assertThat(allowed)
                .isFalse();
    }
}
