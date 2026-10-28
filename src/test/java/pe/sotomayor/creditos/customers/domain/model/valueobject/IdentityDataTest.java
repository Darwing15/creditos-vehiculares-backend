package pe.sotomayor.creditos.customers.domain.model.valueobject;

import org.junit.jupiter.api.Test;

import pe.sotomayor.creditos.customers.domain.exception.IdentityLookupException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class IdentityDataTest {

    @Test
    void shouldPreserveCompoundNamesWhenNormalizingWhitespace() {

        /* Arrange */

        var data = new IdentityData(
                "00000001",
                "  MARÍA   JOSÉ ",
                "DE LA CRUZ",
                "MUÑOZ-PÉREZ",
                "FACTILIZA"
        );

        /* Act */

        var name = data.fullName();

        /* Assert */

        assertThat(name).isEqualTo("MARÍA JOSÉ DE LA CRUZ MUÑOZ-PÉREZ");
        assertThat(data.documentNumber())
                .isEqualTo("00000001");
    }

    @Test
    void shouldAllowOneSurnameWhenSecondSurnameIsMissing() {

        /* Arrange */

        var data = new IdentityData(
                "00000001",
                "NOMBRES",
                "APELLIDO",
                null,
                "FACTILIZA"
        );
        /* Act */

        var name = data.fullName();

        /* Assert */

        assertThat(name)
                .isEqualTo("NOMBRES APELLIDO");
    }

    @Test
    void shouldRejectResultWhenNamesAreEmpty() {

        /* Arrange */

        String names = " ";

        /* Act */

        var failure = catchThrowable(() -> new IdentityData("00000001", names, "UNO", "DOS", "FACTILIZA"));

        /* Assert */

        assertThat(failure).isInstanceOf(IdentityLookupException.class);
    }

    @Test
    void shouldRejectResultWhenCompleteNameExceedsCustomerLength() {

        /* Arrange */

        String names = "A".repeat(150);

        /* Act */

        var failure = catchThrowable(() -> new IdentityData("00000001", names, "UNO", "DOS", "FACTILIZA"));

        /* Assert */
        assertThat(failure).isInstanceOf(IdentityLookupException.class);
    }
}

