package pe.sotomayor.creditos.credit.application.service.credit;

import org.junit.jupiter.api.Test;
import pe.sotomayor.creditos.credit.domain.service.credit.UuidCreditCodeGenerator;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidCreditCodeGeneratorTest {

    private final UuidCreditCodeGenerator generator =
            new UuidCreditCodeGenerator();

    @Test
    void shouldGenerateCreditCodeWhenRequested() {

        /* Arrange */

        /* Act */
        UUID creditCode = generator.generate();

        /* Assert */
        assertThat(creditCode)
                .isNotNull();

    }

    @Test
    void shouldGenerateDifferentCreditCodesOnConsecutiveCalls() {

        /* Arrange */

        /* Act */
        UUID firstCreditCode = generator.generate();
        UUID secondCreditCode = generator.generate();

        /* Assert */
        assertThat(firstCreditCode)
                .isNotEqualTo(secondCreditCode);

    }

    @Test
    void shouldGenerateVersionFourUuidWhenRequested() {

        /* Arrange */

        /* Act */
        UUID creditCode = generator.generate();

        /* Assert */
        assertThat(creditCode.version())
                .isEqualTo(4);

    }

}
