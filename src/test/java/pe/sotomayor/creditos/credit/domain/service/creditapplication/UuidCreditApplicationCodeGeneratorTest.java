package pe.sotomayor.creditos.credit.domain.service.creditapplication;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidCreditApplicationCodeGeneratorTest {

    @Test
    void shouldGenerateCreditApplicationCode() {

        /* Arrange */

        CreditApplicationCodeGenerator codeGenerator =
                new UuidCreditApplicationCodeGenerator();

        /* Act */

        UUID applicationCode =
                codeGenerator.generate();

        /* Assert */

        assertThat(applicationCode)
                .isNotNull();
    }

}
