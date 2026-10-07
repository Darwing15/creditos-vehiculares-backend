package pe.sotomayor.creditos.credit.domain.service.creditapplication;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SequentialCreditApplicationNumberGeneratorTest {

    @Test
    void shouldGenerateCreditApplicationNumberFromSequence() {

        /* Arrange */

        CreditApplicationSequenceProvider sequenceProvider =
                () -> 1L;

        CreditApplicationNumberGenerator numberGenerator =
                new SequentialCreditApplicationNumberGenerator(
                        sequenceProvider
                );

        /* Act */

        String applicationNumber =
                numberGenerator.generate();

        /* Assert */

        assertThat(applicationNumber)
                .isEqualTo("CA-00000001");
    }

    @Test
    void shouldPreserveSequenceValueWhenGeneratingCreditApplicationNumber() {

        /* Arrange */

        CreditApplicationSequenceProvider sequenceProvider =
                () -> 42L;

        CreditApplicationNumberGenerator numberGenerator =
                new SequentialCreditApplicationNumberGenerator(
                        sequenceProvider
                );

        /* Act */

        String applicationNumber =
                numberGenerator.generate();

        /* Assert */

        assertThat(applicationNumber)
                .isEqualTo("CA-00000042");
    }
}
