package pe.sotomayor.creditos.credit.infrastructure.persistence.creditapplication;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.context.annotation.Import;

import pe.sotomayor.creditos.credit.domain.service.creditapplication
        .CreditApplicationSequenceProvider;

import pe.sotomayor.creditos.support.PostgresTestContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        PostgresTestContainer.class,
        PostgresCreditApplicationSequenceProvider.class
})
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class PostgresCreditApplicationSequenceProviderTest {

    @Autowired
    private CreditApplicationSequenceProvider sequenceProvider;

    @Test
    void shouldReturnConsecutiveValuesFromPostgresSequence() {

        /* Arrange */

        /* Act */

        long firstValue =
                sequenceProvider.nextValue();

        long secondValue =
                sequenceProvider.nextValue();

        /* Assert */

        assertThat(secondValue)
                .isEqualTo(firstValue + 1);
    }

}
