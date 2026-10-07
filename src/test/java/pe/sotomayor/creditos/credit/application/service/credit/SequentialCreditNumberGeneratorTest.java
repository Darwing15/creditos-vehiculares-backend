package pe.sotomayor.creditos.credit.application.service.credit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.credit.domain.service.credit.CreditSequenceProvider;
import pe.sotomayor.creditos.credit.domain.service.credit.SequentialCreditNumberGenerator;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SequentialCreditNumberGeneratorTest {

    @Mock
    private CreditSequenceProvider sequenceProvider;

    @InjectMocks
    private SequentialCreditNumberGenerator generator;

    @Test
    void shouldGenerateFormattedCreditNumberWhenSequenceIsAvailable() {

        /* Arrange */
        given(sequenceProvider.nextValue())
                .willReturn(1L);

        /* Act */
        String creditNumber = generator.generate();

        /* Assert */
        assertThat(creditNumber)
                .isEqualTo("CR-00000001");

        verify(sequenceProvider)
                .nextValue();

        verifyNoMoreInteractions(sequenceProvider);

    }

    @Test
    void shouldGenerateFormattedCreditNumberWithLeadingZeros() {

        /* Arrange */
        given(sequenceProvider.nextValue())
                .willReturn(125L);

        /* Act */
        String creditNumber = generator.generate();

        /* Assert */
        assertThat(creditNumber)
                .isEqualTo("CR-00000125");

        verify(sequenceProvider)
                .nextValue();

        verifyNoMoreInteractions(sequenceProvider);

    }

    @Test
    void shouldGenerateFormattedCreditNumberWhenSequenceHasManyDigits() {

        /* Arrange */
        given(sequenceProvider.nextValue())
                .willReturn(12345678L);

        /* Act */
        String creditNumber = generator.generate();

        /* Assert */
        assertThat(creditNumber)
                .isEqualTo("CR-12345678");

        verify(sequenceProvider)
                .nextValue();

        verifyNoMoreInteractions(sequenceProvider);

    }

}
