package uk.gov.pay.adminusers.utils.date;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static java.time.ZonedDateTime.parse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.pay.adminusers.utils.date.DisputeEvidenceDueByDateUtil.getPayDueByDate;

class DisputeEvidenceDueByDateUtilTest {

    @ParameterizedTest
    @MethodSource("epochProvider")
    void shouldCorrectlyCalculatePayDueByDate(ZonedDateTime dateTime, DayOfWeek expected) {
        assertThat(getPayDueByDate(dateTime).getDayOfWeek(), is(expected));
    }

    private static Stream<Arguments> epochProvider() {
        return Stream.of(
                Arguments.of(parse("2022-03-07T13:00:00.000Z"), DayOfWeek.FRIDAY),
                Arguments.of(parse("2022-03-08T13:00:00.000Z"), DayOfWeek.FRIDAY),
                Arguments.of(parse("2022-03-09T13:00:00.000Z"), DayOfWeek.MONDAY),
                Arguments.of(parse("2022-03-10T13:00:00.000Z"), DayOfWeek.TUESDAY),
                Arguments.of(parse("2022-03-11T13:00:00.000Z"), DayOfWeek.WEDNESDAY)
        );
    }
}
