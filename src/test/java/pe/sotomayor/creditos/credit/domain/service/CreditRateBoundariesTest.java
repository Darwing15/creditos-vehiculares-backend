package pe.sotomayor.creditos.credit.domain.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CreditRateBoundariesTest {
    private final CreditCalculator calculator = new DefaultCreditCalculator();

    @ParameterizedTest
    @CsvSource({
            "3499.99,4,0.00", "3499.99,5,2.00", "3500.00,5,2.00",
            "4499.99,5,2.00", "4500.00,5,1.80", "5499.99,5,1.80", "5500.00,5,1.70",
            "6999.99,5,1.70", "7000.00,5,1.60", "9499.99,5,1.60", "9500.00,5,1.50",
            "11499.99,5,1.50", "11500.00,5,1.40", "13499.99,5,1.40", "13500.00,5,1.30",
            "15500.00,5,1.30", "15500.01,5,1.30", "50000.00,12,1.30", "50000.00,4,0.00"
    })
    void selectsRateAtEachBoundary(String financed, int months, String expected) {

        /* Arrange */

        var amount = new BigDecimal(financed);

        /* Act */

        var actual = calculator.calculateCompensatoryInterestRate(amount, months);

        /* Assert */

        assertThat(actual).isEqualByComparingTo(expected);
    }
}
