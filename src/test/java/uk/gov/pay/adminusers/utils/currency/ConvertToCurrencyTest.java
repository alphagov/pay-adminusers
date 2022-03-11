package uk.gov.pay.adminusers.utils.currency;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.adminusers.utils.currency.ConvertToCurrency.convertPenceToPounds;

class ConvertToCurrencyTest {


    @ParameterizedTest
    @MethodSource("penceProvider")
    void shouldCorrectlyCalculateConvertPenceToPounds(Long pence, String expected) {
        assertThat(convertPenceToPounds.apply(pence).toString(), is(expected));
    }

    private static Stream<Arguments> penceProvider() {
        return Stream.of(
                Arguments.of(123L, "1.23"),
                Arguments.of(12000L, "120.00"),
                Arguments.of(1L, "0.01"),
                Arguments.of(0L, "0.00"),
                Arguments.of(-1500L, "-15.00")
        );
    }
}
