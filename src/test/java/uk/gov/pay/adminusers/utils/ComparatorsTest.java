package uk.gov.pay.adminusers.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.adminusers.utils.Comparators.numericallyThenLexicographically;
import static uk.gov.pay.adminusers.utils.Comparators.usingNumericComparator;

public class ComparatorsTest {

    @Test
    public void shouldOrderNumericStringsInAscendingOrder() {
        List<String> result = Stream.of("1", "6", "4", "10", "5")
                .sorted(usingNumericComparator())
                .collect(Collectors.toList());
        assertThat(result, is(Arrays.asList("1","4","5","6","10")));
    }

    @Test
    public void shouldOrderGatewayAccountsIdsNumericallyThenLexicographically() {
        List<String> result = Stream.of("1aaa","1", "6", "cde", "4", "bbb23", "10", "5")
                .sorted(numericallyThenLexicographically())
                .collect(Collectors.toList());
        assertThat(result, is(Arrays.asList("1","4","5","6","10","1aaa","bbb23","cde")));
    }

}
